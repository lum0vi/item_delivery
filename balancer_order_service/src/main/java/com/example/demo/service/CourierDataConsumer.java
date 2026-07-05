package com.example.demo.service;

import com.example.demo.dto.CourierDataOrder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierDataConsumer {

    private final KafkaReceiver<String, CourierDataOrder> kafkaReceiver;
    private final CourierDataOrderService courierDataOrderService;


    @PostConstruct
    public void start() {
        kafkaReceiver.receive()
                .flatMap(record ->
                        courierDataOrderService.process(record.value())
                                .doOnSuccess(v -> record.receiverOffset().acknowledge())
                                .onErrorResume(error -> {
                                    log.error("Ошибка при обработке сообщения, пропускаем: ", error);
                                    // Подтверждаем офсет сбойного сообщения, чтобы поток не останавливался
                                    record.receiverOffset().acknowledge();
                                    return Mono.empty();
                                })
                )
                .retryWhen(Retry.indefinitely().doBeforeRetry(sig ->
                        log.warn("Перезапуск реактивного потока Kafka из-за ошибки: {}", sig.failure().getMessage())
                ))
                .subscribe(
                        null,
                        error -> log.error("Критический сбой реактивного Kafka Потока: ", error),
                        () -> log.info("Реактивный Kafka Поток завершил работу.")
                );
    }
}
