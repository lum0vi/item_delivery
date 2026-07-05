package com.example.demo.controller;

import com.example.demo.DemoApplication;
import com.example.demo.auth.CurrentUser;
import com.example.demo.model.ClientConnection;
import com.example.demo.service.JwtTokenService;
import com.example.demo.service.WebSocketRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Component
@Slf4j
public class NotificationWebSocketHandler implements WebSocketHandler {

    private final WebSocketRegistry registry;
    private final JwtTokenService jwtTokenService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public NotificationWebSocketHandler(
            WebSocketRegistry registry,
            JwtTokenService jwtTokenService,
            @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.registry = registry;
        this.jwtTokenService = jwtTokenService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        return currentUser(session)
                .flatMap(user -> {
                    String username = user.username();
                    ClientConnection connection = new ClientConnection();
                    registry.add(username, connection);
                    updateSessionInRedis(username).subscribe();
                    Mono<Void> output = session.send(
                            connection.outbound()
                                    .map(session::textMessage)
                    );
                    Mono<Void> input = session.receive()
                            .doOnNext(msg -> {
                                log.info(msg.getPayloadAsText());
                                updateSessionInRedis(username).subscribe();
                            })
                            .then();
                    return Mono.when(output, input)
                            .doFinally(signal -> {
                                registry.remove(username);
                                connection.close();

                                redisTemplate.opsForValue()
                                        .delete(username)
                                        .subscribe();
                            });
                });
    }

    private Mono<CurrentUser> currentUser(WebSocketSession session) {
        String authorizationHeader = session.getHandshakeInfo()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return unauthorized(session, "Authorization header is missing or invalid");
        }

        String token = authorizationHeader.substring(7);

        return jwtTokenService.parseAccessToken(token)
                .onErrorResume(e -> unauthorized(session, "Invalid access token"));
    }

    // Метод теперь возвращает корректную цепочку закрытия
    private Mono<CurrentUser> unauthorized(WebSocketSession session, String reason) {
        return session.close(CloseStatus.POLICY_VIOLATION.withReason(reason))
                .then(Mono.empty());
    }

    private Mono<Void> updateSessionInRedis(String username) {
        return redisTemplate.opsForValue()
                .set(username, DemoApplication.nameService, Duration.ofMinutes(20))
                .then();
    }
}