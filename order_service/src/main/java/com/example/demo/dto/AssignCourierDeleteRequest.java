package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignCourierDeleteRequest(@NotBlank String courier_name) {
}
