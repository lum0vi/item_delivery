package com.example.demo.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaAuthEventPublisher implements AuthEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaAuthEventPublisher.class);

    private final KafkaTemplate<String, AuthEvent> kafkaTemplate;
    private final String topic;

    public KafkaAuthEventPublisher(
            KafkaTemplate<String, AuthEvent> kafkaTemplate,
            @Value("${app.kafka.auth-topic:auth.events}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public Mono<Void> publish(AuthEvent event) {
        return Mono.fromFuture(kafkaTemplate.send(topic, event.username(), event))
                .doOnSuccess(result -> log.debug("Published auth event to topic {}", topic))
                .doOnError(error -> log.warn("Kafka publish failed: {}", error.getMessage()))
                .onErrorResume(error -> Mono.empty())
                .then();
    }
}
