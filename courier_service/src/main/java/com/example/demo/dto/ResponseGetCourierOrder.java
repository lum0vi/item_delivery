package com.example.demo.dto;

public record ResponseGetCourierOrder (
        Long order,
        String status,
        Long code
){
}
