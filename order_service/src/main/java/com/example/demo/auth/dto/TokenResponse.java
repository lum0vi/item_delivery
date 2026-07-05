package com.example.demo.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String role
) {
}
