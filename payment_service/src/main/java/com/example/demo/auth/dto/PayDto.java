package com.example.demo.auth.dto;


import java.time.LocalDate;

public record PayDto (
        Long id,
        String method,
        LocalDate date_payment,
        String user,
        Long order
) {
}
