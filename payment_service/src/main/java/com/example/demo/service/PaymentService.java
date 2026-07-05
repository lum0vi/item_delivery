package com.example.demo.service;

import com.example.demo.auth.CurrentUser;
import com.example.demo.auth.dto.PayDto;
import com.example.demo.model.Payment;
import com.example.demo.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    // Получить платеж по ID (с обработкой ошибки, если не найден)
    public Mono<Payment> getById(String username, Long id) {
        return paymentRepository.findById(id)
                .filter(payment -> payment.getUser().equals(username))
                .switchIfEmpty(Mono.error(() ->
                        new ResourceNotFoundException("Payment not found with id: " + id)));
    }

    // Получить все платежи пользователя
    public Flux<Payment> getByUserId(CurrentUser user) {
        return paymentRepository.findByUser(user.username());
    }

    // Создать новый платеж
    @Transactional
    public Mono<Payment> create(Payment payment) {
        payment.setId(null);

        if (payment.getDatePayment() == null) {
            payment.setDatePayment(LocalDate.now());
        }

        return paymentRepository.save(payment);
    }

    // Обновить существующий платеж
//    @Transactional
//    public Mono<Payment> update(Long id, Payment paymentDetails) {
//        return getById(id) // Проверяем существование перед обновлением
//                .flatMap(existingPayment -> {
//                    existingPayment.setUser(paymentDetails.getUser());
//                    existingPayment.setOrderId(paymentDetails.getOrderId());
//                    existingPayment.setPaymentMethod(paymentDetails.getPaymentMethod());
//                    // дату обычно не обновляют, но при необходимости:
//                    // existingPayment.setDatePayment(paymentDetails.getDatePayment());
//                    return paymentRepository.save(existingPayment);
//                });
//    }

    // Удалить платеж
    @Transactional
    public Mono<Void> delete(String username, Long id) {
        return paymentRepository.findById(id)
                .filter(payment -> payment.getUser().equals(username))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Это не ваш платеж")))
                .flatMap(payment -> paymentRepository.deleteById(payment.getId()));
    }


    public Mono<PayDto> getPaymentByOrderId(String username, Long order_id){
        return paymentRepository.findByOrderId(order_id)
                .filter(payment -> payment.getUser().equals(username))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Это не ваш платеж")))
                .map(payment -> new PayDto(
                        payment.getId(),
                        payment.getPaymentMethod(),
                        payment.getDatePayment(),
                        payment.getUser(),
                        payment.getOrderId()));
    }
}
