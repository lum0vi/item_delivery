package com.example.demo.service;


import com.example.demo.DemoApplication;
import com.example.demo.config.RedisChannels;
import com.example.demo.dto.CourierDataOrder;
import com.example.demo.dto.CourierDataOrderUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

    private final ReactiveRedisMessageListenerContainer listenerContainer;
    private final ObjectMapper objectMapper;
    private final CourierEventHandler courierEventHandler;

    @PostConstruct
    public void subscribe() {
        listenerContainer
                .receive(ChannelTopic.of(DemoApplication.nameService))
                .flatMap(this::deserialize)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Received empty or invalid Redis message");
                    return Mono.empty();
                }))
                .flatMap(courierEventHandler::handle)
                .doOnSubscribe(subscription ->
                        log.info("Subscribed to Redis channel '{}'", RedisChannels.COURIER_EVENTS)
                )
                .doOnError(error ->
                        log.error("Redis subscriber error", error)
                )
                .retry()
                .subscribe();
    }

    private Mono<CourierDataOrderUser> deserialize(
            ReactiveSubscription.Message<String, String> message
    ) {
        try {
            CourierDataOrderUser event =
                    objectMapper.readValue(
                            message.getMessage(),
                            CourierDataOrderUser.class
                    );
            return Mono.just(event);
        } catch (Exception e) {
            log.error("Cannot deserialize Redis message", e);
            return Mono.empty();
        }
    }
}