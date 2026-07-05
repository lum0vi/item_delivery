package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;

public final class RedisChannels {

    private RedisChannels() {
    }

//    @Value("${app.redis_channels.channels.order_service_main:order_service_main}")
    public static final String COURIER_EVENTS = "order_service_main";

}