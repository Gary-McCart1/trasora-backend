package com.example.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username or email is required")
    private String login; // username or email

    @NotBlank(message = "Password is required")
    private String password;
}

