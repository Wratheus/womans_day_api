package com.womansday.api.service;

import com.womansday.api.dto.request.CreateTaskRequest;
import com.womansday.api.dto.response.TaskResponse;
import com.womansday.api.entity.*;
import com.womansday.api.enums.Role;
import com.womansday.api.enums.SubmissionStatus;
import com.womansday.api.enums.TaskType;
import com.womansday.api.exception.BusinessLogicException;
import com.womansday.api.exception.ResourceNotFoundException;
import com.womansday.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskSubmissionRepository submissionRepository;
    @Mock private SubmissionPhotoRepository photoRepository;
    @Mock private UserRepository userRepository;
    @Mock private PhotoStorageService photoStorageService;

    @InjectMocks
    private TaskService taskService;

    private Task textTask;
    private Task photoTask;
    private Task collabTask;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        textTask = Task.builder()
                .id(1L).title("Text Task").reward(300).type(TaskType.TEXT).collaborative(false).build();
        photoTask = Task.builder()
                .id(2L).title("Photo Task").reward(500).type(TaskType.PHOTO).collaborative(false).build();
        collabTask = Task.builder()
                .id(3L).title("Collab Task").reward(600).type(TaskType.PHOTO).collaborative(true).build();
        user1 = User.builder()
                .id(1L).login("user1").firstName("Alice").lastName("Smith").department("IT").role(Role.USER).build();
        user2 = User.builder()
                .id(2L).login("user2").firstName("Bob").lastName("Jones").department("HR").role(Role.USER).build();
    }

    @Test
    void submitTask_shouldRejectTextTaskWithoutText() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(textTask));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(submissionRepository.hasActiveSubmission(1L, 1L)).thenReturn(false);

        assertThrows(BusinessLogicException.class,
                () -> taskService.submitTask(1L, 1L, null, null, null));
    }

    @Test
    void submitTask_shouldRejectIfUserHasActiveSubmission() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(textTask));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(submissionRepository.hasActiveSubmission(1L, 1L)).thenReturn(true);

        assertThrows(BusinessLogicException.class,
                () -> taskService.submitTask(1L, 1L, "my answer", null, null));
    }

    @Test
    void submitTask_shouldRejectParticipantsForNonCollabTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(textTask));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        assertThrows(BusinessLogicException.class,
                () -> taskService.submitTask(1L, 1L, "answer", null, List.of(2L)));
    }

    @Test
    void submitTask_shouldRejectIfPendingParticipantHasPendingInvitation() {
        when(taskRepository.findById(3L)).thenReturn(Optional.of(collabTask));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(submissionRepository.hasActiveSubmission(1L, 3L)).thenReturn(false);
        when(submissionRepository.hasActiveSubmission(2L, 3L)).thenReturn(false);
        when(submissionRepository.hasPendingInvitation(2L, 3L)).thenReturn(true);

        assertThrows(BusinessLogicException.class,
                () -> taskService.submitTask(3L, 1L, null, null, List.of(2L)));
    }

    @Test
    void acceptInvitation_shouldMoveFromPendingToParticipants() {
        TaskSubmission submission = TaskSubmission.builder()
                .id(1L).submitter(user1).task(collabTask)
                .status(SubmissionStatus.WAITING_FOR_PARTICIPANTS)
                .participants(new HashSet<>(Set.of(user1)))
                .pendingParticipants(new HashSet<>(Set.of(user2)))
                .photos(new ArrayList<>())
                .build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(submissionRepository.hasActiveSubmission(2L, 3L)).thenReturn(false);

        taskService.acceptInvitation(1L, 2L);

        assertTrue(submission.getParticipants().contains(user2));
        assertFalse(submission.getPendingParticipants().contains(user2));
        assertEquals(SubmissionStatus.PENDING, submission.getStatus());
    }

    @Test
    void declineInvitation_shouldSetPendingWhenAllDeclined() {
        TaskSubmission submission = TaskSubmission.builder()
                .id(1L).submitter(user1).task(collabTask)
                .status(SubmissionStatus.WAITING_FOR_PARTICIPANTS)
                .participants(new HashSet<>(Set.of(user1)))
                .pendingParticipants(new HashSet<>(Set.of(user2)))
                .photos(new ArrayList<>())
                .build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        taskService.declineInvitation(1L, 2L);

        // Should go to PENDING, not CANCELLED (submitter is still a participant)
        assertEquals(SubmissionStatus.PENDING, submission.getStatus());
        assertFalse(submission.getPendingParticipants().contains(user2));
    }

    @Test
    void reviewSubmission_shouldApprove() {
        TaskSubmission submission = TaskSubmission.builder()
                .id(1L).submitter(user1).task(textTask)
                .status(SubmissionStatus.PENDING)
                .participants(new HashSet<>(Set.of(user1)))
                .pendingParticipants(new HashSet<>())
                .photos(new ArrayList<>())
                .text("Answer")
                .build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));

        taskService.reviewSubmission(1L, true);

        assertEquals(SubmissionStatus.APPROVED, submission.getStatus());
    }

    @Test
    void reviewSubmission_shouldRejectAndCleanupPhotos() {
        SubmissionPhoto photo = SubmissionPhoto.builder()
                .id(1L).filePath("/tmp/photo.jpg").contentType("image/jpeg").build();

        TaskSubmission submission = TaskSubmission.builder()
                .id(1L).submitter(user1).task(photoTask)
                .status(SubmissionStatus.PENDING)
                .participants(new HashSet<>(Set.of(user1)))
                .pendingParticipants(new HashSet<>())
                .photos(new ArrayList<>(List.of(photo)))
                .build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));

        taskService.reviewSubmission(1L, false);

        assertEquals(SubmissionStatus.REJECTED, submission.getStatus());
    }

    @Test
    void reviewSubmission_shouldThrowIfAlreadyReviewed() {
        TaskSubmission submission = TaskSubmission.builder()
                .id(1L).submitter(user1).task(textTask)
                .status(SubmissionStatus.APPROVED)
                .participants(new HashSet<>(Set.of(user1)))
                .pendingParticipants(new HashSet<>())
                .photos(new ArrayList<>())
                .build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));

        assertThrows(BusinessLogicException.class, () -> taskService.reviewSubmission(1L, true));
    }

    @Test
    void cancelSubmission_shouldOnlyAllowSubmitter() {
        TaskSubmission submission = TaskSubmission.builder()
                .id(1L).submitter(user1).task(textTask)
                .status(SubmissionStatus.PENDING)
                .participants(new HashSet<>(Set.of(user1)))
                .pendingParticipants(new HashSet<>())
                .photos(new ArrayList<>())
                .build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));

        assertThrows(BusinessLogicException.class,
                () -> taskService.cancelSubmission(1L, 2L)); // user2 is not submitter
    }

    @Test
    void cancelSubmission_shouldCancelPendingSubmission() {
        TaskSubmission submission = TaskSubmission.builder()
                .id(1L).submitter(user1).task(textTask)
                .status(SubmissionStatus.PENDING)
                .participants(new HashSet<>(Set.of(user1)))
                .pendingParticipants(new HashSet<>())
                .photos(new ArrayList<>())
                .build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));

        taskService.cancelSubmission(1L, 1L);

        assertEquals(SubmissionStatus.CANCELLED, submission.getStatus());
    }

    @Test
    void cancelSubmission_shouldRejectForApprovedSubmission() {
        TaskSubmission submission = TaskSubmission.builder()
                .id(1L).submitter(user1).task(textTask)
                .status(SubmissionStatus.APPROVED)
                .participants(new HashSet<>(Set.of(user1)))
                .pendingParticipants(new HashSet<>())
                .photos(new ArrayList<>())
                .build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));

        assertThrows(BusinessLogicException.class,
                () -> taskService.cancelSubmission(1L, 1L));
    }

    @Test
    void createTask_shouldCreateAndReturn() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("New Task");
        request.setDescription("Description");
        request.setReward(100);
        request.setType(TaskType.TEXT);
        request.setCollaborative(false);

        Task saved = Task.builder()
                .id(10L).title("New Task").description("Description")
                .reward(100).type(TaskType.TEXT).collaborative(false).build();

        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        TaskResponse response = taskService.createTask(request);

        assertEquals(10L, response.getId());
        assertEquals("New Task", response.getTitle());
    }

    @Test
    void deleteTask_shouldThrowIfActiveSubmissions() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(textTask));
        when(submissionRepository.hasActiveSubmissionsForTask(1L)).thenReturn(true);

        assertThrows(BusinessLogicException.class, () -> taskService.deleteTask(1L));
    }

    @Test
    void deleteTask_shouldDeleteWhenNoActiveSubmissions() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(textTask));
        when(submissionRepository.hasActiveSubmissionsForTask(1L)).thenReturn(false);

        taskService.deleteTask(1L);

        verify(taskRepository).delete(textTask);
    }

    @Test
    void getLeaderboard_shouldReturnSortedByEarned() {
        when(userRepository.findByRoleNot(Role.ADMIN)).thenReturn(List.of(user1, user2));
        when(submissionRepository.sumApprovedRewardsByUserId(1L)).thenReturn(300L);
        when(submissionRepository.sumApprovedRewardsByUserId(2L)).thenReturn(500L);

        var leaderboard = taskService.getLeaderboard();

        assertEquals(2, leaderboard.size());
        assertEquals(2L, leaderboard.get(0).getUserId()); // user2 has more earned
        assertEquals(1, leaderboard.get(0).getRank());
        assertEquals(2, leaderboard.get(1).getRank());
    }
}
