package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderItemDto (
         Long id,
         String name,
         int price,
         int quantity,
         String description,
         String supplier
){
}
