package com.example.demo.service;

import com.example.demo.dto.CourierDataOrder;
import com.example.demo.dto.CourierDataOrderUser;
import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
public class CourierDataOrderService {

    private final OrderRepository orderRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RedisPublisher redisPublisher;

    public CourierDataOrderService(OrderRepository orderRepository,
                                   @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate,
                                   RedisPublisher redisPublisher){
        this.redisPublisher = redisPublisher;
        this.orderRepository = orderRepository;
        this.redisTemplate = redisTemplate;
    }

    public Mono<Void> process(CourierDataOrder courierDataOrder) {
        log.info("process - {}", courierDataOrder.order_id());
        return getUsernameForOrder(courierDataOrder)
                .flatMap(orderUser ->
                        redisTemplate.opsForValue()
                                .get(orderUser.username())
                                .switchIfEmpty(Mono.error(
                                        new IllegalStateException(
                                                "User " + orderUser.username() + " is not connected"
                                        )
                                ))
                                .flatMap(serviceName -> {

                                    log.info("User connected to service: {}", serviceName);

                                    return redisPublisher.publish(serviceName, orderUser);
                                })
                )
                .then();
    }


//    @Cacheable(value="getUsernameForOrder", key = "#orderid")
    private Mono<CourierDataOrderUser> getUsernameForOrder(CourierDataOrder courierDataOrder){
        String key = "order:user:" + courierDataOrder.order_id();
        return redisTemplate.opsForValue().get(key).switchIfEmpty(
                orderRepository.findById(courierDataOrder.order_id()).map(order -> {
                    return new CourierDataOrderUser(courierDataOrder.location(),
                            courierDataOrder.courier_name(), courierDataOrder.order_id(), courierDataOrder.status(),
                            courierDataOrder.code(), order.getUser());
                }).flatMap(courierDataOrderUser ->
                    redisTemplate.opsForValue()
                            .set(key, courierDataOrderUser.username(), Duration.ofMinutes(20))
                            .thenReturn(courierDataOrderUser.username())
                )
        ).map(username -> new CourierDataOrderUser(courierDataOrder.location(),
                courierDataOrder.courier_name(), courierDataOrder.order_id(), courierDataOrder.status(),
                courierDataOrder.code(), username));
//        return orderRepository.findById(courierDataOrder.order_id()).map(order -> {
//            return new CourierDataOrderUser(courierDataOrder.location(),
//                    courierDataOrder.courier_name(), courierDataOrder.order_id(), courierDataOrder.status(),
//                    courierDataOrder.code(), order.getUser());
//        });
    }
}
