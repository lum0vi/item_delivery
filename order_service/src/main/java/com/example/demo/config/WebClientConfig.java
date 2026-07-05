package com.example.demo.config;


import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@Getter
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Value("${URL_PAYMENT_SERVICE:http://localhost:8082}")
    private String url_payment_service;

    @Bean
    public WebClient dataWebClient(WebClient.Builder builder) {
        return builder.baseUrl(url_payment_service).build();
    }
}
