package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record RequestCreateOrderItem(
        @NotBlank String name,
        @NotBlank int price,
        @NotBlank int quantity,
        @NotBlank String description
) {
}
