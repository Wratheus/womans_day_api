package com.womansday.api.service;

import com.womansday.api.dto.response.BudgetResponse;
import com.womansday.api.dto.response.SubmissionResponse;
import com.womansday.api.dto.response.TaskResponse;
import com.womansday.api.entity.*;
import com.womansday.api.enums.SubmissionStatus;
import com.womansday.api.enums.TaskType;
import com.womansday.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskSubmissionRepository submissionRepository;
    private final SubmissionPhotoRepository photoRepository;
    private final UserRepository userRepository;

    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::toTaskResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse getTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Задание не найдено"));
        return toTaskResponse(task);
    }

    @Transactional
    public SubmissionResponse submitTask(Long taskId, Long userId, String text, List<MultipartFile> photos) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Задание не найдено"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        if (submissionRepository.existsByUserIdAndTaskId(userId, taskId)) {
            throw new IllegalArgumentException("Вы уже отправили ответ на это задание");
        }

        validateSubmission(task.getType(), text, photos);

        TaskSubmission submission = TaskSubmission.builder()
                .user(user)
                .task(task)
                .status(SubmissionStatus.PENDING)
                .text(text)
                .build();

        submission = submissionRepository.save(submission);

        if (photos != null && !photos.isEmpty()) {
            for (MultipartFile photo : photos) {
                try {
                    SubmissionPhoto photoEntity = SubmissionPhoto.builder()
                            .submission(submission)
                            .data(photo.getBytes())
                            .contentType(photo.getContentType())
                            .build();
                    photoRepository.save(photoEntity);
                    submission.getPhotos().add(photoEntity);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка загрузки фото", e);
                }
            }
        }

        return toSubmissionResponse(submission);
    }

    @Transactional
    public SubmissionResponse reviewSubmission(Long taskId, Long userId, boolean approved) {
        TaskSubmission submission = submissionRepository.findByUserIdAndTaskId(userId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("Ответ не найден"));

        submission.setStatus(approved ? SubmissionStatus.APPROVED : SubmissionStatus.REJECTED);
        submissionRepository.save(submission);

        return toSubmissionResponse(submission);
    }

    public BudgetResponse getBudget() {
        long totalTasks = taskRepository.sumAllRewards();
        long userCount = userRepository.count();
        long totalBudget = totalTasks * userCount;
        long approvedBudget = submissionRepository.sumRewardsByStatus(SubmissionStatus.APPROVED);

        return BudgetResponse.builder()
                .totalBudget(totalBudget)
                .approvedBudget(approvedBudget)
                .build();
    }

    public byte[] getPhotoData(Long photoId) {
        SubmissionPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Фото не найдено"));
        return photo.getData();
    }

    public String getPhotoContentType(Long photoId) {
        SubmissionPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Фото не найдено"));
        return photo.getContentType();
    }

    private void validateSubmission(TaskType taskType, String text, List<MultipartFile> photos) {
        boolean hasText = text != null && !text.isBlank();
        boolean hasPhotos = photos != null && !photos.isEmpty();

        switch (taskType) {
            case TEXT -> {
                if (!hasText) throw new IllegalArgumentException("Это задание требует текстовый ответ");
            }
            case PHOTO -> {
                if (!hasPhotos) throw new IllegalArgumentException("Это задание требует фото");
            }
            case TEXT_AND_PHOTO -> {
                if (!hasText) throw new IllegalArgumentException("Это задание требует текстовый ответ");
                if (!hasPhotos) throw new IllegalArgumentException("Это задание требует фото");
            }
        }
    }

    private TaskResponse toTaskResponse(Task task) {
        List<TaskSubmission> submissions = submissionRepository.findByTaskId(task.getId());

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .reward(task.getReward())
                .type(task.getType())
                .submissions(submissions.stream()
                        .map(this::toSubmissionResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private SubmissionResponse toSubmissionResponse(TaskSubmission submission) {
        List<Long> photoIds = submission.getPhotos().stream()
                .map(SubmissionPhoto::getId)
                .collect(Collectors.toList());

        return SubmissionResponse.builder()
                .id(submission.getId())
                .userId(submission.getUser().getId())
                .userLogin(submission.getUser().getLogin())
                .userDepartment(submission.getUser().getDepartment())
                .status(submission.getStatus())
                .text(submission.getText())
                .createdAt(submission.getCreatedAt())
                .photoIds(photoIds)
                .build();
    }
}
