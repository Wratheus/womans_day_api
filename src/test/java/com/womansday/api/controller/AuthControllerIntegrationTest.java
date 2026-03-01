package com.womansday.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.womansday.api.dto.request.LoginRequest;
import com.womansday.api.dto.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_shouldReturnCreatedWithTokens() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setLogin("newuser");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setDepartment("Engineering");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.login").value("newuser"))
                .andExpect(jsonPath("$.role").value("user"));
    }

    @Test
    void register_shouldRejectDuplicateLogin() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setLogin("duplicate");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setDepartment("IT");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldValidateFields() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setLogin("ab"); // too short
        request.setPassword("short");  // too short
        request.setFirstName("");
        request.setLastName("");
        request.setDepartment("");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void login_shouldReturnTokensForAdmin() throws Exception {
        // Admin is created by DataInitializer
        LoginRequest request = new LoginRequest();
        request.setLogin("admin");
        request.setPassword("adminpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.role").value("admin"));
    }

    @Test
    void login_shouldRejectInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setLogin("admin");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoint_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
