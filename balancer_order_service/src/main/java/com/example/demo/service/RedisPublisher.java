package com.example.demo.service;

import com.example.demo.config.RedisChannels;
import com.example.demo.dto.CourierDataOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RedisPublisher {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisPublisher(@Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate,
                          ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    public Mono<Long> publish(String channel, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            return redisTemplate.convertAndSend(channel, json);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}