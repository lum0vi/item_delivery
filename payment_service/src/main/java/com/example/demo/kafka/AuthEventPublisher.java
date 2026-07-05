package com.example.demo.kafka;

import reactor.core.publisher.Mono;

public interface AuthEventPublisher {

    Mono<Void> publish(AuthEvent event);
}
