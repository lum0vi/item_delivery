package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.auth.CurrentUser;
import com.example.demo.auth.dto.TokenResponse;
import com.example.demo.config.JwtProperties;
import com.example.demo.model.AppUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@Service
public class JwtTokenService {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final JwtProperties jwtProperties;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.accessKey = Keys.hmacShaKeyFor(jwtProperties.getAccessToken().getSecret().getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(jwtProperties.getRefreshToken().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenResponse issueTokens(AppUser user) {
        return new TokenResponse(
                createToken(user.getUsername(), user.getRole(), "access", accessKey, jwtProperties.getAccessToken().getExpirationMs()),
                createToken(user.getUsername(), user.getRole(), "refresh", refreshKey, jwtProperties.getRefreshToken().getExpirationMs()),
                user.getRole()
        );
    }

    public Mono<CurrentUser> parseAccessToken(String token) {
        return parse(token, accessKey, "access");
    }

    public Mono<CurrentUser> parseRefreshToken(String token) {
        return parse(token, refreshKey, "refresh");
    }

    private Mono<CurrentUser> parse(String token, SecretKey key, String expectedType) {
        return Mono.fromCallable(() -> {
                    Claims claims = Jwts.parser()
                            .verifyWith(key)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

                    String tokenType = claims.get("type", String.class);
                    if (!expectedType.equals(tokenType)) {
                        throw new JwtException("Unexpected token type");
                    }

                    return new CurrentUser(claims.getSubject(), claims.get("role", String.class));
                })
                .onErrorMap(
                        JwtException.class,
                        error -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token", error)
                );
    }

    private String createToken(
            String username,
            String role,
            String tokenType,
            SecretKey key,
            long expirationMs
    ) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("type", tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }
}
