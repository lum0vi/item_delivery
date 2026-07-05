package com.example.demo.dto;

public record OrderDetailsForCourier(
        Long order,
        //Long payment,
        String address,
        String username, // тот для кого везется заказ
        Long ratingCourier,
        String action
) {
}
