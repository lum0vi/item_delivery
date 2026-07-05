package com.example.demo.service;


import com.example.demo.dto.CourierDataOrder;
import com.example.demo.dto.OrderDetailsForCourier;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.sender.KafkaSender;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierConsumer {

    private final KafkaReceiver<String, OrderDetailsForCourier> kafkaReceiver;
    private final CourierService courierService;
//    private final KafkaSender<String, CourierDataOrder> kafkaSender;

    @PostConstruct
    public void start() {

        kafkaReceiver.receive()
                .flatMap(record ->
                        courierService.process(record.value())
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
