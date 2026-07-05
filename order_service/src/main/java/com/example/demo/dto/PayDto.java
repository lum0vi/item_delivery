package com.example.demo.dto;

import java.time.LocalDate;

public record PayDto (
        Long id,
        String method,
        LocalDate date_payment,
        String user,
        Long order
) {
}
