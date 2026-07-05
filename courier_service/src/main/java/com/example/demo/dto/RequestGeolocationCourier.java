package com.example.demo.dto;

public record RequestGeolocationCourier (
        double longitude,
        double latitude,
        Long order_id
) {
}
