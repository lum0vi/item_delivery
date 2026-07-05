package com.example.demo.dto;


import jakarta.validation.constraints.NotBlank;

public record PayOrderDto(
        @NotBlank Long OrderId,
        @NotBlank String PaymentMethod
) {
}
