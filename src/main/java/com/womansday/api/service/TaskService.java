package com.womansday.api.service;

import com.womansday.api.dto.response.*;
import com.womansday.api.entity.*;
import com.womansday.api.enums.Role;
import com.womansday.api.enums.SubmissionStatus;
import com.womansday.api.enums.TaskType;
import com.womansday.api.exception.BusinessLogicException;
import com.womansday.api.exception.ResourceNotFoundException;
import com.womansday.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class TaskService {

    private static final int MAX_PHOTOS_PER_SUBMISSION = 5;
    private static final long MAX_PHOTO_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final int MAX_TEXT_LENGTH = 5000;

    private final TaskRepository taskRepository;
    private final TaskSubmissionRepository submissionRepository;
    private final SubmissionPhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final PhotoStorageService photoStorageService;

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks(Long userId) {
        return taskRepository.findAll().stream()
                .map(task -> toTaskResponse(task, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Задание не найдено"));
        return toTaskResponse(task, userId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SubmissionResponse submitTask(Long taskId, Long submitterId, String text,
                                          List<MultipartFile> photos, List<Long> participantIds) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Задание не найдено"));

        User submitter = userRepository.findById(submitterId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        // Abuse protection
        if (text != null && text.length() > MAX_TEXT_LENGTH) {
            throw new BusinessLogicException("Текст не должен превышать " + MAX_TEXT_LENGTH + " символов");
        }
        if (photos != null && photos.size() > MAX_PHOTOS_PER_SUBMISSION) {
            throw new BusinessLogicException("Максимум " + MAX_PHOTOS_PER_SUBMISSION + " фото на ответ");
        }

        Set<User> participants = new HashSet<>();
        participants.add(submitter);

        if (participantIds != null && !participantIds.isEmpty()) {
            for (Long pid : participantIds) {
                User participant = userRepository.findById(pid)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Участник с ID " + pid + " не найден"));
                participants.add(participant);
            }
        }

        for (User participant : participants) {
            if (submissionRepository.hasActiveSubmission(participant.getId(), taskId)) {
                throw new BusinessLogicException(
                        "Пользователь " + participant.getLogin() +
                                " уже имеет активный ответ на это задание");
            }
        }

        validateSubmission(task.getType(), text, photos);

        TaskSubmission submission = TaskSubmission.builder()
                .submitter(submitter)
                .task(task)
                .status(SubmissionStatus.PENDING)
                .text(text)
                .participants(participants)
                .build();

        submission = submissionRepository.save(submission);

        if (photos != null && !photos.isEmpty()) {
            for (MultipartFile photo : photos) {
                String contentType = photo.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new BusinessLogicException("Допускаются только изображения");
                }
                if (photo.getSize() > MAX_PHOTO_SIZE_BYTES) {
                    throw new BusinessLogicException("Размер фото не должен превышать 10MB");
                }
                try {
                    String filePath = photoStorageService.store(
                            submission.getId(), contentType, photo.getBytes());
                    SubmissionPhoto photoEntity = SubmissionPhoto.builder()
                            .submission(submission)
                            .filePath(filePath)
                            .contentType(contentType)
                            .build();
                    photoRepository.save(photoEntity);
                    submission.getPhotos().add(photoEntity);
                } catch (IOException e) {
                    throw new BusinessLogicException("Ошибка загрузки фото");
                }
            }
        }

        return toSubmissionResponse(submission);
    }

    @Transactional
    public AdminSubmissionResponse reviewSubmission(Long submissionId, boolean approved) {
        TaskSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Ответ не найден"));

        if (submission.getStatus() != SubmissionStatus.PENDING) {
            throw new BusinessLogicException("Этот ответ уже был рассмотрен");
        }

        submission.setStatus(approved ? SubmissionStatus.APPROVED : SubmissionStatus.REJECTED);
        submissionRepository.save(submission);

        return toAdminSubmissionResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<AdminSubmissionResponse> getAdminSubmissions(String statusFilter) {
        List<TaskSubmission> submissions;

        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                SubmissionStatus status = SubmissionStatus.valueOf(statusFilter.toUpperCase());
                submissions = submissionRepository.findByStatusOrderByCreatedAtEpochAsc(status);
            } catch (IllegalArgumentException e) {
                throw new BusinessLogicException("Неизвестный статус: " + statusFilter);
            }
        } else {
            submissions = submissionRepository.findAllOrderByStatusAndCreatedAtEpoch();
        }

        return submissions.stream()
                .map(this::toAdminSubmissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudget() {
        long totalRewards = taskRepository.sumAllRewards();
        long userCount = userRepository.countByRoleNot(Role.ADMIN);
        long totalBudget = totalRewards * userCount;
        long approvedBudget = submissionRepository.sumRewardsByStatus(SubmissionStatus.APPROVED);

        return BudgetResponse.builder()
                .totalBudget(totalBudget)
                .approvedBudget(approvedBudget)
                .build();
    }

    @Transactional(readOnly = true)
    public SubmissionPhoto getPhoto(Long photoId, Long userId, String role) {
        if (Role.ADMIN.name().equals(role)) {
            return photoRepository.findById(photoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Фото не найдено"));
        }
        return photoRepository.findByIdAndParticipant(photoId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Фото не найдено"));
    }

    private void validateSubmission(TaskType taskType, String text, List<MultipartFile> photos) {
        boolean hasText = text != null && !text.isBlank();
        boolean hasPhotos = photos != null && !photos.isEmpty();

        switch (taskType) {
            case TEXT -> {
                if (!hasText) throw new BusinessLogicException("Это задание требует текстовый ответ");
            }
            case PHOTO -> {
                if (!hasPhotos) throw new BusinessLogicException("Это задание требует фото");
            }
            case TEXT_AND_PHOTO -> {
                if (!hasText) throw new BusinessLogicException("Это задание требует текстовый ответ");
                if (!hasPhotos) throw new BusinessLogicException("Это задание требует фото");
            }
        }
    }

    private TaskResponse toTaskResponse(Task task, Long userId) {
        List<TaskSubmission> userSubmissions =
                submissionRepository.findByParticipantAndTaskId(userId, task.getId());

        String myStatus = SubmissionStatus.NOT_STARTED.value();
        SubmissionResponse mySubmission = null;

        if (!userSubmissions.isEmpty()) {
            TaskSubmission latest = userSubmissions.get(0);
            myStatus = latest.getStatus().name();
            mySubmission = toSubmissionResponse(latest);
        }

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .reward(task.getReward())
                .type(task.getType())
                .collaborative(task.getCollaborative())
                .myStatus(myStatus)
                .mySubmission(mySubmission)
                .build();
    }

    private SubmissionResponse toSubmissionResponse(TaskSubmission submission) {
        return SubmissionResponse.builder()
                .id(submission.getId())
                .submitterId(submission.getSubmitter().getId())
                .submitterLogin(submission.getSubmitter().getLogin())
                .submitterFirstName(submission.getSubmitter().getFirstName())
                .submitterLastName(submission.getSubmitter().getLastName())
                .submitterDepartment(submission.getSubmitter().getDepartment())
                .participants(submission.getParticipants().stream()
                        .map(u -> UserResponse.builder()
                                .id(u.getId())
                                .login(u.getLogin())
                                .firstName(u.getFirstName())
                                .lastName(u.getLastName())
                                .department(u.getDepartment())
                                .avatarUrl(u.getAvatarPath() != null ? "/api/users/" + u.getId() + "/avatar" : null)
                                .build())
                        .collect(Collectors.toList()))
                .status(submission.getStatus())
                .text(submission.getText())
                .createdAtEpoch(submission.getCreatedAtEpoch())
                .photoUrls(submission.getPhotos().stream()
                        .map(p -> "/api/tasks/photos/" + p.getId())
                        .collect(Collectors.toList()))
                .build();
    }

    private AdminSubmissionResponse toAdminSubmissionResponse(TaskSubmission submission) {
        return AdminSubmissionResponse.builder()
                .id(submission.getId())
                .taskId(submission.getTask().getId())
                .taskTitle(submission.getTask().getTitle())
                .taskReward(submission.getTask().getReward())
                .submitterId(submission.getSubmitter().getId())
                .submitterLogin(submission.getSubmitter().getLogin())
                .submitterFirstName(submission.getSubmitter().getFirstName())
                .submitterLastName(submission.getSubmitter().getLastName())
                .submitterDepartment(submission.getSubmitter().getDepartment())
                .participants(submission.getParticipants().stream()
                        .map(u -> UserResponse.builder()
                                .id(u.getId())
                                .login(u.getLogin())
                                .firstName(u.getFirstName())
                                .lastName(u.getLastName())
                                .department(u.getDepartment())
                                .avatarUrl(u.getAvatarPath() != null ? "/api/users/" + u.getId() + "/avatar" : null)
                                .build())
                        .collect(Collectors.toList()))
                .status(submission.getStatus())
                .text(submission.getText())
                .createdAtEpoch(submission.getCreatedAtEpoch())
                .photoUrls(submission.getPhotos().stream()
                        .map(p -> "/api/tasks/photos/" + p.getId())
                        .collect(Collectors.toList()))
                .build();
    }
}
