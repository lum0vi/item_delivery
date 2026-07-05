package com.example.demo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RequestPayDto (
        @NotBlank String method,
        @NotBlank Long order,
        @NotBlank String username
) {
}
