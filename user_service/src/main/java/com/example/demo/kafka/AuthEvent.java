package com.example.demo.kafka;

import java.time.Instant;

public record AuthEvent(
        String username,
        String role,
        String eventType,
        Instant occurredAt
) {
}
