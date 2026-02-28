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
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        return toTaskResponse(task, userId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SubmissionResponse submitTask(Long taskId, Long submitterId, String text,
                                          List<MultipartFile> photos, List<Long> participantIds) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        User submitter = userRepository.findById(submitterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (text != null && text.length() > MAX_TEXT_LENGTH) {
            throw new BusinessLogicException("Text must not exceed " + MAX_TEXT_LENGTH + " characters");
        }
        if (photos != null && photos.size() > MAX_PHOTOS_PER_SUBMISSION) {
            throw new BusinessLogicException("Maximum " + MAX_PHOTOS_PER_SUBMISSION + " photos per submission");
        }

        Set<User> participants = new HashSet<>();
        participants.add(submitter);

        Set<User> pendingParticipants = new HashSet<>();

        if (participantIds != null && !participantIds.isEmpty()) {
            if (!Boolean.TRUE.equals(task.getCollaborative())) {
                throw new BusinessLogicException("This task does not support co-participation");
            }
            for (Long pid : participantIds) {
                if (pid.equals(submitterId)) continue;
                User participant = userRepository.findById(pid)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Participant with ID " + pid + " not found"));
                if (submitter.getRole() == Role.USER && participant.getRole() == Role.ADMIN) {
                    throw new BusinessLogicException("Cannot add admin as participant");
                }
                pendingParticipants.add(participant);
            }
        }

        for (User participant : participants) {
            if (submissionRepository.hasActiveSubmission(participant.getId(), taskId)) {
                throw new BusinessLogicException(
                        "User " + participant.getLogin() +
                                " already has an active submission for this task");
            }
        }
        for (User pending : pendingParticipants) {
            if (submissionRepository.hasActiveSubmission(pending.getId(), taskId)) {
                throw new BusinessLogicException(
                        "User " + pending.getLogin() +
                                " already has an active submission for this task");
            }
        }

        validateSubmission(task.getType(), text, photos);

        SubmissionStatus initialStatus = pendingParticipants.isEmpty()
                ? SubmissionStatus.PENDING
                : SubmissionStatus.WAITING_FOR_PARTICIPANTS;

        TaskSubmission submission = TaskSubmission.builder()
                .submitter(submitter)
                .task(task)
                .status(initialStatus)
                .text(text)
                .participants(participants)
                .pendingParticipants(pendingParticipants)
                .build();

        submission = submissionRepository.save(submission);

        if (photos != null && !photos.isEmpty()) {
            for (MultipartFile photo : photos) {
                String contentType = photo.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new BusinessLogicException("Only image files are allowed");
                }
                if (photo.getSize() > MAX_PHOTO_SIZE_BYTES) {
                    throw new BusinessLogicException("Photo must not exceed 10MB");
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
                    throw new BusinessLogicException("Photo upload failed");
                }
            }
        }

        return toSubmissionResponse(submission);
    }

    @Transactional
    public void acceptInvitation(Long submissionId, Long userId) {
        TaskSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        if (submission.getStatus() != SubmissionStatus.WAITING_FOR_PARTICIPANTS) {
            throw new BusinessLogicException("This submission is not waiting for participants");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!submission.getPendingParticipants().contains(user)) {
            throw new BusinessLogicException("You are not invited to this submission");
        }

        if (submissionRepository.hasActiveSubmission(userId, submission.getTask().getId())) {
            throw new BusinessLogicException("You already have an active submission for this task");
        }

        submission.getPendingParticipants().remove(user);
        submission.getParticipants().add(user);

        if (submission.getPendingParticipants().isEmpty()) {
            submission.setStatus(SubmissionStatus.PENDING);
        }

        submissionRepository.save(submission);
    }

    @Transactional
    public void declineInvitation(Long submissionId, Long userId) {
        TaskSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        if (submission.getStatus() != SubmissionStatus.WAITING_FOR_PARTICIPANTS) {
            throw new BusinessLogicException("This submission is not waiting for participants");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!submission.getPendingParticipants().contains(user)) {
            throw new BusinessLogicException("You are not invited to this submission");
        }

        submission.getPendingParticipants().remove(user);

        if (submission.getPendingParticipants().isEmpty()) {
            boolean hasAcceptedParticipants = submission.getParticipants().size() > 1;
            if (hasAcceptedParticipants) {
                submission.setStatus(SubmissionStatus.PENDING);
            } else {
                submission.setStatus(SubmissionStatus.CANCELLED);
            }
        }

        submissionRepository.save(submission);
    }

    @Transactional
    public AdminSubmissionResponse reviewSubmission(Long submissionId, boolean approved) {
        TaskSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        if (submission.getStatus() != SubmissionStatus.PENDING) {
            throw new BusinessLogicException("This submission has already been reviewed");
        }

        submission.setStatus(approved ? SubmissionStatus.APPROVED : SubmissionStatus.REJECTED);

        if (!approved) {
            submission.getPendingParticipants().clear();
        }

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
                throw new BusinessLogicException("Unknown status: " + statusFilter);
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
                    .orElseThrow(() -> new ResourceNotFoundException("Photo not found"));
        }
        return photoRepository.findByIdAndParticipant(photoId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found"));
    }

    private void validateSubmission(TaskType taskType, String text, List<MultipartFile> photos) {
        boolean hasText = text != null && !text.isBlank();
        boolean hasPhotos = photos != null && !photos.isEmpty();

        switch (taskType) {
            case TEXT -> {
                if (!hasText) throw new BusinessLogicException("This task requires a text answer");
            }
            case PHOTO -> {
                if (!hasPhotos) throw new BusinessLogicException("This task requires a photo");
            }
            case TEXT_AND_PHOTO -> {
                if (!hasText) throw new BusinessLogicException("This task requires a text answer");
                if (!hasPhotos) throw new BusinessLogicException("This task requires a photo");
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
            myStatus = latest.getStatus().value();
            mySubmission = toSubmissionResponse(latest);
        } else {
            List<TaskSubmission> pendingInvitations =
                    submissionRepository.findByPendingParticipantAndTaskId(userId, task.getId());
            if (!pendingInvitations.isEmpty()) {
                TaskSubmission invitation = pendingInvitations.get(0);
                myStatus = "invited";
                mySubmission = toSubmissionResponse(invitation);
            }
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
                                .hasAvatar(u.getAvatarPath() != null)
                                .build())
                        .collect(Collectors.toList()))
                .pendingParticipants(submission.getPendingParticipants().stream()
                        .map(u -> UserResponse.builder()
                                .id(u.getId())
                                .login(u.getLogin())
                                .firstName(u.getFirstName())
                                .lastName(u.getLastName())
                                .department(u.getDepartment())
                                .hasAvatar(u.getAvatarPath() != null)
                                .build())
                        .collect(Collectors.toList()))
                .status(submission.getStatus())
                .text(submission.getText())
                .createdAtEpoch(submission.getCreatedAtEpoch())
                .photoIds(submission.getPhotos().stream()
                        .map(SubmissionPhoto::getId)
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
                                .hasAvatar(u.getAvatarPath() != null)
                                .build())
                        .collect(Collectors.toList()))
                .pendingParticipants(submission.getPendingParticipants().stream()
                        .map(u -> UserResponse.builder()
                                .id(u.getId())
                                .login(u.getLogin())
                                .firstName(u.getFirstName())
                                .lastName(u.getLastName())
                                .department(u.getDepartment())
                                .hasAvatar(u.getAvatarPath() != null)
                                .build())
                        .collect(Collectors.toList()))
                .status(submission.getStatus())
                .text(submission.getText())
                .createdAtEpoch(submission.getCreatedAtEpoch())
                .photoIds(submission.getPhotos().stream()
                        .map(SubmissionPhoto::getId)
                        .collect(Collectors.toList()))
                .build();
    }
}
