package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentServicePostRequest(
        @NotBlank String method,
        @NotBlank Long order,
        @NotBlank String username
) {
}
