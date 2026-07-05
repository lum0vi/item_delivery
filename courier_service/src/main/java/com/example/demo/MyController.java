package com.example.demo;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import com.example.demo.dto.*;
import com.example.demo.service.CourierAssignmentService;
import com.example.demo.service.CourierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.example.demo.auth.CurrentUser;
import com.example.demo.middleware.JwtAuthenticationWebFilter;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Slf4j
@RestController
@RequestMapping("/controller")
@RequiredArgsConstructor
public class MyController {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final KafkaSender<String, CourierDataOrder> kafkaSender;
    private final CourierService courierService;
    private final CourierAssignmentService courierAssignmentService;

    @Value("${app.kafka.topics.order:order-events}")
    private String topic_order_events;

    @Value("${app.kafka.topics.payment-order:payment-events}")
    private String topic_payment_events;


    //Сохранение геопозиции курьера в Redis и асинхронная отправка данных в Kafka.
    @PostMapping("/location")
    public Mono<Void> LocationCourier(ServerWebExchange exchange, @RequestBody RequestGeolocationCourier req) {
        CurrentUser user = requireRole(exchange, Set.of("COURIER"));
        Location location = new Location(req.latitude(), req.longitude());
        String key = "user:" + user.username();
        log.info(user.username());
        log.info(user.role());
//        return redisTemplate.opsForValue().set(key, location, Duration.ofMinutes(10))
        return Mono.just(1)
                .then(getCourierOrderUser(user.username(), "start"))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Активный заказ курьера не найден")))
                .flatMap(resp -> {

                    CourierDataOrder payload = new CourierDataOrder(
                            new Location(req.latitude(), req.longitude()),
                            user.username(),
                            resp.order(),
                            resp.status(),
                            resp.code()
                    );

                    SenderRecord<String, CourierDataOrder, String> record = SenderRecord.create(
                            new ProducerRecord<>(topic_payment_events, String.valueOf(resp.order()), payload),
                            String.valueOf(resp.order())
                    );

                    log.info("Отправка координат курьера {} по заказу №{} в Kafka", user.username(), resp.order());
                    return kafkaSender.send(Mono.just(record)).then();
                });
    }


    //Получить информацию о текущем заказе курьера со статусом 'start'.
    @GetMapping("/order_courier")
    public Mono<ResponseGetCourierOrder> GetCourierOrder(ServerWebExchange exchange) {
        CurrentUser user = requireRole(exchange, Set.of("COURIER"));
        return courierService.getCourierOrder(user.username(), "start")
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "У вас нет активных заказов в статусе 'start'")));
    }

    @GetMapping("/orders")
    public Flux<ResponseGetCourierOrder> GetOrdersCourier(ServerWebExchange exchange){
        CurrentUser user = requireRole(exchange, Set.of("COURIER"));
        return courierService.getCourierOrders(user.username());
    }


    //Завершение заказа курьером с обязательной проверкой секретного кода.
    // Пример запроса: POST /controller/order_courier_done?code=123456
    @PostMapping("/order_courier_done")
    public Mono<Void> OrderCourierDone(ServerWebExchange exchange, @RequestParam Long code) {
        CurrentUser user = requireRole(exchange, Set.of("COURIER"));

        return courierService.getCourierOrder(user.username(), "start")
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Активный заказ для завершения не найден")))
                .flatMap(resp -> courierAssignmentService.completeDelivery(resp.order(), user.username(), code)
                        // Если код не совпал, транслируем RuntimeException в статус 400 Bad Request
                        .onErrorMap(e -> new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()))
                )
                .doOnSuccess(v -> log.info("Курьер {} успешно закрыл заказ с кодом проверки", user.username()));
    }

    // fail order courier

    public Mono<ResponseGetCourierOrder> getCourierOrderMono(String username, String status) {
        return courierService.getCourierOrder(username, status);
    }

    public Mono<ResponseGetCourierOrder> getCourierOrderUser(String username, String status){
        return courierService.getCourierOrderUser(username, status);
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
        boolean tf = false;
        log.info(currentUser.role());
        for (String s : allowedRoles){
            if (s.equals(currentUser.role())){
                tf = true;
                break;
            }
        }
        if (!tf) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return currentUser;
    }
}
