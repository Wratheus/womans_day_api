package com.womansday.api.dto.response;

import com.womansday.api.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {
    private Long id;
    private String login;
    private String department;
    private Role role;
    private String token;
}
