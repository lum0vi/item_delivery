package com.example.demo;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.example.demo.auth.dto.PayDto;
import com.example.demo.auth.dto.RequestPayDto;
import com.example.demo.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.example.demo.auth.CurrentUser;
import com.example.demo.middleware.JwtAuthenticationWebFilter;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor // Автоматически создаст конструктор для paymentService
@RestController
@RequestMapping("/controller")
public class Payment {

    private static final Logger log = LoggerFactory.getLogger(Payment.class);

    private final PaymentService paymentService;

    @PostMapping("/pay")
    public Mono<PayDto> CreatePayment(ServerWebExchange exchange, @RequestBody RequestPayDto req) {
        if ("QR".equals(req.method())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR method is not supported");
        }

        CurrentUser user = currentUser(exchange);
        log.info("Processing payment for user: {}", user.username());
        log.info("pr: {}", user.username());
        return paymentService.create(new com.example.demo.model.Payment(
                        null,
                        req.username(),
                        LocalDate.now(),
                        req.order(),
                        req.method()
                ))
                .map(savedPayment -> new PayDto(
                        savedPayment.getId(),
                        savedPayment.getPaymentMethod(),
                        savedPayment.getDatePayment(),
                        savedPayment.getUser(),
                        savedPayment.getOrderId()
                ));
    }

    @GetMapping("/pay")
    public Mono<List<PayDto>> GetAllPayment(ServerWebExchange exchange){
        return paymentService.getByUserId(currentUser(exchange)).map(payment -> {
            return new PayDto(
                    payment.getId(),
                    payment.getPaymentMethod(),
                    payment.getDatePayment(),
                    payment.getUser(),
                    payment.getOrderId()
            );
        }).collectList();
    }

    @GetMapping("/pay/{id}")
    public Mono<PayDto> GetPaymentById(ServerWebExchange exchange, @PathVariable("id") Long id) {
        CurrentUser user = currentUser(exchange);
        return paymentService.getById(user.username(), id)
                .filter(payment -> payment.getUser().equals(user.username())).map(payment -> {
                    return new PayDto(
                            payment.getId(),
                            payment.getPaymentMethod(),
                            payment.getDatePayment(),
                            payment.getUser(),
                            payment.getOrderId()
                    );
                }).switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Это не ваш платеж")));
    }

    @GetMapping("/pay/order/{id}")
    public Mono<PayDto> GetPaymentByOrderId(ServerWebExchange exchange, @PathVariable("id") Long order_id) {
        CurrentUser user = currentUser(exchange);
        return paymentService.getPaymentByOrderId(user.username(), order_id);
    }

    @DeleteMapping("/pay/{id}")
    public Mono<Void> DeletePaymentById(ServerWebExchange exchange, @PathVariable("id") Long id){
        CurrentUser user = currentUser(exchange);
        return paymentService.delete(user.username(), id);
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
