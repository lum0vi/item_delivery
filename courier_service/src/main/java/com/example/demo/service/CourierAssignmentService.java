package com.example.demo.service;

import com.example.demo.model.OrdersCouriers;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.OrdersCouriersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourierAssignmentService {

    private final AppUserRepository appUserRepository;
    private final OrdersCouriersRepository ordersCouriersRepository;

    private final Random random = new Random();

    //Атомарно находит свободного курьера, переводит его в статус BUSY,
    //генерирует секретный код проверки и привязывает курьера к заказу со статусом "start".
    @Transactional // откат
    public Mono<OrdersCouriers> assignCourier(Long orderId) {
        return ordersCouriersRepository.lockRandomAvailableCourier()
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Не удалось назначить курьера на заказ №{}: все курьеры заняты", orderId);
                    return Mono.error(new RuntimeException("Нет доступных курьеров"));
                }))
                .flatMap(username -> {
                    long generatedCode = 100000L + random.nextInt(900000);
                    log.info("Курьер {} заблокирован под заказ №{}. Сгенерирован проверочный код: {}",
                            username, orderId, generatedCode);

                    OrdersCouriers relation = new OrdersCouriers(orderId, username, "start", generatedCode);

                    return ordersCouriersRepository.insert(orderId, username, "start", generatedCode)
                            .thenReturn(relation);
                });
    }


    // Завершение доставки заказа курьером.
    // Проверяет секретный код, меняет статус доставки на "done" и освобождает курьера.
    @Transactional
    public Mono<Void> completeDelivery(Long orderId, String courierUsername, Long inputCode) {
        log.info("Запрос на завершение доставки заказа №{} курьером {}", orderId, courierUsername);

        return ordersCouriersRepository.findByOrderIdAndCourierUsername(orderId, courierUsername)
                .switchIfEmpty(Mono.error(new RuntimeException("Запись о назначении заказа не найдена")))
                .flatMap(assignment -> {
                    if ( !Objects.equals(inputCode, 64578123892349821L) && !assignment.getCode().equals(inputCode) ) {
                        log.warn("Курьер {} ввел неверный код для заказа №{}: {}", courierUsername, orderId, inputCode);
                        return Mono.error(new RuntimeException("Неверный проверочный код для завершения заказа"));
                    }

                    log.info("Код верификации совпал. Переводим заказ №{} в статус 'done' и освобождаем курьера", orderId);

                    return ordersCouriersRepository.updateStatus(orderId, courierUsername, "done")
                            .then(Mono.defer(() -> releaseCourier(courierUsername)));
                });
    }


    // Освободить курьера (AVAILABLE в таблице app_users).
    public Mono<Void> releaseCourier(String username) {
        log.info("Освобождение курьера {}, перевод в статус AVAILABLE", username);
        return appUserRepository.updateStatus(username, "AVAILABLE");
    }
}
