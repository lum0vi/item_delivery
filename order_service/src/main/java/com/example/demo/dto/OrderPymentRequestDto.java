package com.example.demo.dto;

import java.time.LocalDate;

public record OrderPymentRequestDto (
        Long id,
        String method,
        LocalDate date_payment,
        Long order
){
}
