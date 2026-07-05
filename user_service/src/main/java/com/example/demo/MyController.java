package com.example.demo;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.example.demo.auth.CurrentUser;
import com.example.demo.middleware.JwtAuthenticationWebFilter;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/controller")
public class MyController {


    private static final Logger log = LoggerFactory.getLogger(MyController.class);

    @GetMapping("/hello")
    public Mono<String> sayHello(ServerWebExchange exchange) {
        CurrentUser currentUser = currentUser(exchange);
        System.out.println(currentUser);
        return Mono.just("Hello from Reactive WebFlux, %s! role=%s".formatted(
                currentUser.username(),
                currentUser.role()
        ));
    }

    @GetMapping("/user/profile")
    public Mono<String> userProfile(ServerWebExchange exchange) {
        CurrentUser currentUser = currentUser(exchange);
        return Mono.just("Profile for %s with role %s".formatted(
                currentUser.username(),
                currentUser.role()
        ));
    }

    @GetMapping("/admin/panel")
    public Mono<String> adminPanel(ServerWebExchange exchange) {
        CurrentUser currentUser = requireRole(exchange, Set.of("ADMIN"));
        return Mono.just("Admin panel for %s".formatted(currentUser.username()));
    }

    @GetMapping("/supplier/portal")
    public Mono<String> supplierPortal(ServerWebExchange exchange) {
        CurrentUser currentUser = requireRole(exchange, Set.of("SUPPLIER"));
        return Mono.just("Supplier portal for %s".formatted(currentUser.username()));
    }

    private CurrentUser currentUser(ServerWebExchange exchange) {
        CurrentUser currentUser = exchange.getAttribute(JwtAuthenticationWebFilter.CURRENT_USER_ATTRIBUTE);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user is missing");
        }
        return currentUser;
    }

    private CurrentUser requireRole(ServerWebExchange exchange, Set<String> allowedRoles) {
        CurrentUser currentUser = currentUser(exchange);
        if (!allowedRoles.contains(currentUser.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return currentUser;
    }
}
