package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record RequestCreateAssignCourier(
        @NotBlank Long order_id
        // username
) {
}
