package com.womansday.api.dto.response;

import com.womansday.api.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class AdminSubmissionResponse {
    private Long id;
    private Long taskId;
    private String taskTitle;
    private Integer taskReward;
    private Long submitterId;
    private String submitterLogin;
    private String submitterFirstName;
    private String submitterLastName;
    private String submitterDepartment;
    private List<UserResponse> participants;
    private List<UserResponse> pendingParticipants;
    private SubmissionStatus status;
    private String text;
    private Long createdAtEpoch;
    private List<Long> photoIds;
}
