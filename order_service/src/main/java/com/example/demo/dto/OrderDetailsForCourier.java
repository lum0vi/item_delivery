package com.example.demo.dto;

public record OrderDetailsForCourier (
        Long order,
        //Long payment,
        String address,
        String username,
        Long ratingCourier,
        String action
) {
}
