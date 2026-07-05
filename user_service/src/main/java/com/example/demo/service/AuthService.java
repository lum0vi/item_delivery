package com.example.demo.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.auth.CurrentUser;
import com.example.demo.auth.dto.TokenResponse;
import com.example.demo.kafka.AuthEvent;
import com.example.demo.kafka.AuthEventPublisher;
import com.example.demo.model.AppUser;

import reactor.core.publisher.Mono;

@Service
public class AuthService {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final AuthEventPublisher authEventPublisher;

    public AuthService(
            UserService userService,
            JwtTokenService jwtTokenService,
            AuthEventPublisher authEventPublisher
    ) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.authEventPublisher = authEventPublisher;
    }

    public Mono<String> deleteUser(String token){
        return jwtTokenService.parseRefreshToken(token)
                .map(CurrentUser::username)
                .flatMap(userService::findByUsername)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")))
                .map(this::deleteUser);
    }

    public Mono<TokenResponse> createUser(AppUser user){
        //AppUser u = userService.saveUser(user).map(saveUser -> {return saveUser;});
        return userService.saveUser(user).flatMap(saveUser -> {
           return login(saveUser.getUsername(), saveUser.getPassword()) ;
        });
    }

    public Mono<TokenResponse> login(String username, String password) {
        return userService.findByUsername(username)
                .filter(user -> user.getPassword().equals(password))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")))
                .flatMap(this::issueTokensAfterLoginEvent);
    }

    public Mono<TokenResponse> refresh(String refreshToken) {
        return jwtTokenService.parseRefreshToken(refreshToken)
                .map(CurrentUser::username)
                .flatMap(userService::findByUsername)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")))
                .map(jwtTokenService::issueTokens);
    }

    private String deleteUser(AppUser user){
        userService.deleteUser(user);
        return "ok";
    }

    private Mono<TokenResponse> issueTokensAfterLoginEvent(AppUser user) {
        TokenResponse response = jwtTokenService.issueTokens(user);
        AuthEvent event = new AuthEvent(user.getUsername(), user.getRole(), "LOGIN", Instant.now());
        return authEventPublisher.publish(event).thenReturn(response);
    }
}
