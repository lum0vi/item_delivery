package com.example.demo.middleware;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.example.demo.auth.CurrentUser;
import com.example.demo.service.JwtTokenService;

import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationWebFilter implements WebFilter {

    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login",
            "/auth/refresh"
    );

    private final JwtTokenService jwtTokenService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationWebFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (PUBLIC_PATHS.contains(path) || !pathMatcher.match("/controller/**", path)) {
            return chain.filter(exchange);
        }

        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Authorization header is missing or invalid");
        }

        String token = authorizationHeader.substring(7);
        return jwtTokenService.parseAccessToken(token)
                .onErrorResume(error -> unauthorized(exchange, "Invalid access token").then(Mono.<CurrentUser>empty()))
                .flatMap(currentUser -> continueWithUser(exchange, chain, currentUser));
    }

    private Mono<Void> continueWithUser(
            ServerWebExchange exchange,
            WebFilterChain chain,
            CurrentUser currentUser
    ) {
        exchange.getAttributes().put(CURRENT_USER_ATTRIBUTE, currentUser);
        return chain.filter(exchange);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        byte[] payload = ("{\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
    }
}
