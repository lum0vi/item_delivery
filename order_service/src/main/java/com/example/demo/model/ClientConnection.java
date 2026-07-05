package com.example.demo.model;

import com.example.demo.dto.CourierDataOrderUser;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class ClientConnection {

    private final Sinks.Many<String> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    public void send(String message) {
        sink.tryEmitNext(message);
    }

    public Flux<String> outbound() {
        return sink.asFlux();
    }

    public void close() {
        sink.tryEmitComplete();
    }
}