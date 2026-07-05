package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record RequestCreateOrderDto(
        @NotBlank  String user,
        @NotBlank String address,
        List<OrderItemDto> items
) {
}
