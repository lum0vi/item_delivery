package com.example.demo.dto;

public record CourierDataOrderUser(
        Location location,
        String courier_name,
        Long order_id,
        String status,
        Long code,
        String username
) {
}
