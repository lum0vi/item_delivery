package com.example.demo.service;

import com.example.demo.dto.CourierDataOrderUser;
import com.example.demo.model.ClientConnection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class CourierEventHandler {

    private final WebSocketRegistry webSocketRegistry;
    private final ObjectMapper objectMapper;

    public Mono<Void> handle(CourierDataOrderUser order) {

        ClientConnection clientConnection =
                webSocketRegistry.get(order.username());

        if (clientConnection == null) {
            return Mono.empty();
        }

        return Mono.fromCallable(() ->
                        objectMapper.writeValueAsString(order)
                ).doOnNext(clientConnection::send)
                .then();
    }
}
