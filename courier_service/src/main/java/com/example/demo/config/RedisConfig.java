package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {

        RedisSerializationContext<String, Object> context =
                RedisSerializationContext
                        .<String, Object>newSerializationContext(RedisSerializer.string())
                        .value(RedisSerializer.json())
                        .hashValue(RedisSerializer.json())
                        .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}