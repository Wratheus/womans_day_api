package com.womansday.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class MeResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private Long balance;
    private String avatarUrl;
}
