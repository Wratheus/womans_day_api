package com.womansday.api.dto.response;

import com.womansday.api.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class TaskStatsResponse {
    private Long id;
    private String title;
    private String description;
    private Integer reward;
    private TaskType type;
    private Boolean collaborative;

    private int completedSubmissionsCount;
    private int completedUsersCount;
    private List<CompletedSubmissionEntry> completedSubmissions;

    @Data
    @Builder
    @AllArgsConstructor
    public static class CompletedSubmissionEntry {
        private Long submissionId;
        private Integer earnedReward;
        private Long createdAtEpoch;
        private List<UserResponse> participants;
    }
}
