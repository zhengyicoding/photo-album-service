package com.example.photoalbum.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid") String email) {
}
