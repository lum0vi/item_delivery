package com.example.demo.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingAuthEventPublisher implements AuthEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingAuthEventPublisher.class);

    @Override
    public Mono<Void> publish(AuthEvent event) {
        log.info("Auth event: user={}, role={}, type={}", event.username(), event.role(), event.eventType());
        return Mono.empty();
    }
}
