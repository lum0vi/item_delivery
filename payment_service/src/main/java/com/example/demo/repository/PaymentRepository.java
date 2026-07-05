package com.example.demo.repository;

import com.example.demo.model.Payment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PaymentRepository extends ReactiveCrudRepository<Payment, Long> {

    // Поиск всех платежей конкретного пользователя
    Flux<Payment> findByUser(String user);

    // Поиск платежа по ID заказа
    Mono<Payment> findByOrderId(Long orderId);

    // Поиск всех платежей по методу оплаты
    Flux<Payment> findByPaymentMethod(String paymentMethod);
}
