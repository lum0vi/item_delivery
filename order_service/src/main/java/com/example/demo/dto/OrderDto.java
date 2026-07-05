package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record OrderDto(
        Long id,
        String user,
    String address,
    List<OrderItemDto> items,
    int discount,
    int totalsum,
    LocalDate order_date
) {
}
