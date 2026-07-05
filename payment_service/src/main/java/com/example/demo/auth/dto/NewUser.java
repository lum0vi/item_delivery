package com.example.demo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record NewUser(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String role
) {
}
