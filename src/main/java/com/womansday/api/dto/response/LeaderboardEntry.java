package com.womansday.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LeaderboardEntry {
    private Integer rank;
    private Long userId;
    private String firstName;
    private String lastName;
    private String department;
    private Boolean hasAvatar;
    private Long earned;
    private Boolean hidden;
}
