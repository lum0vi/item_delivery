package com.example.demo.service;


import com.example.demo.auth.CurrentUser;
import com.example.demo.dto.*;
import com.example.demo.model.Order;
import com.example.demo.model.OrdersOrderItem;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.OrdersOrderItemRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrdersOrderItemRepository ordersOrderItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final WebClient paymentServiceClient;

    public OrderService(OrderRepository orderRepository, OrdersOrderItemRepository ordersOrderItemRepository,
                        OrderItemRepository orderItemRepository, WebClient paymentServiceClient){
        this.orderRepository = orderRepository;
        this.ordersOrderItemRepository = ordersOrderItemRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentServiceClient = paymentServiceClient;
    }

    public Mono<OrderDto> createOrder(RequestCreateOrderDto req) {
        Order newOrder = new Order();
        newOrder.setId(null);
        newOrder.setUser(req.user());
        newOrder.setAddress(req.address());
        newOrder.setDiscount(0);

        int sum = 0;
        if (req.items() != null) {
            for (OrderItemDto item : req.items()) {
                sum += item.price() * item.quantity();
            }
        }
        newOrder.setTotalSum(sum);

        return orderRepository.save(newOrder)
                .doOnNext(savedOrder -> log.info("Заказ успешно создан с ID: {}", savedOrder.getId()))
                .flatMap(savedOrder -> {
                    if (req.items() == null || req.items().isEmpty()) {
                        return Mono.just(mapToDto(savedOrder, req.items()));
                    }

                    return Flux.fromIterable(req.items())
                            .flatMap(itemDto -> {
                                OrdersOrderItem link = new OrdersOrderItem();
                                link.setOrderId(savedOrder.getId());
                                link.setOrderItemId(itemDto.id());
                                return ordersOrderItemRepository.save(link);
                            })
                            .then(Mono.just(mapToDto(savedOrder, req.items())));
                })
                .doOnError(error -> log.error("Ошибка при оформлении заказа: {}", error.getMessage()));
    }


    private OrderDto mapToDto(Order order, List<OrderItemDto> items) {
        return new OrderDto(
                order.getId(),
                order.getUser(),
                order.getAddress(),
                items != null ? items : List.of(),
                order.getDiscount(),
                order.getTotalSum(),
                order.getOrderDate() != null ? order.getOrderDate() : LocalDate.now()
        );
    }

    public Mono<OrderDto> getOrder(CurrentUser user, Long id){
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Заказ с ID " + id + " не найден"
                )))
                .filter(currentOrder -> currentOrder.getUser().equals(user.username()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "У вас нет прав на просмотр этого заказа"
                )))
                .flatMap(currentOrder -> {
                    Mono<List<OrderItemDto>> itemsMono = orderItemRepository.findAllByOrderId(currentOrder.getId()).map(
                            orderItem -> {
                                return new OrderItemDto(orderItem.getId(), orderItem.getName(),
                                        orderItem.getPrice(), orderItem.getQuantity(), orderItem.getDescription(), orderItem.getSupplier());
                                }
                            ).collectList();
                    return Mono.zip(Mono.just(currentOrder), itemsMono)
                            .map(tuple -> {
                                Order order = tuple.getT1();
                                List<OrderItemDto> itemsList = tuple.getT2();
                                return new OrderDto(
                                        order.getId(),
                                        order.getUser(),
                                        order.getAddress(),
                                        itemsList,
                                        order.getDiscount(),
                                        order.getTotalSum(),
                                        order.getOrderDate()
                                );
                            });
                });
    }

    public Mono<List<OrderDto>> getOrderUser(CurrentUser user) {
        return orderRepository.findAllByUser(user.username())
                .flatMap(currentOrder -> {
                    return orderItemRepository.findAllByOrderId(currentOrder.getId())
                            .collectList()
                            .map(orderItems -> {

                                List<OrderItemDto> dtoList = orderItems.stream()
                                        .map(item -> new OrderItemDto(
                                                item.getId(),
                                                item.getName(),
                                                item.getPrice(),
                                                item.getQuantity(),
                                                item.getDescription(),
                                                item.getSupplier()
                                        ))
                                        .toList();
                                return new OrderDto(
                                        currentOrder.getId(),
                                        currentOrder.getUser(),
                                        currentOrder.getAddress(),
                                        dtoList,
                                        currentOrder.getDiscount(),
                                        currentOrder.getTotalSum(),
                                        currentOrder.getOrderDate()
                                );
                            });
                })
                .collectList();
    }

    public Mono<PayDto> paymentOrder(CurrentUser user, Long id, OrderPymentRequestDto req, String authorization){
        return paymentServiceClient.post().uri("/controller/pay").header(HttpHeaders.AUTHORIZATION, authorization)
                .bodyValue(new PaymentServicePostRequest(
                req.method(), req.order(), user.username()
        )).retrieve().onStatus(HttpStatusCode::isError, clientResponse ->
                Mono.error(new ResponseStatusException(
                        clientResponse.statusCode(),
                        "Внешний сервис вернул ошибку"
                ))
        ).bodyToMono(PayDto.class);
    }

    public Mono<PayDto> getPaymentOrderId(String token, Long orderid){
        return paymentServiceClient.get().uri("/controller/pay/order/{id}", orderid).header(HttpHeaders.AUTHORIZATION, token)
                .retrieve().onStatus(HttpStatusCode::isError, clientResponse ->
                        Mono.error(new ResponseStatusException(
                                clientResponse.statusCode(),
                                "Внешний сервис вернул ошибку"
                        ))
                ).bodyToMono(PayDto.class);
    }

    public Mono<List<PayDto>> getPaymentOrder(String token){
        return paymentServiceClient.get().uri("/controller/pay").header("Authorization", token)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                Mono.error(new ResponseStatusException(
                        clientResponse.statusCode(),
                        "Внешний сервис вернул ошибку"
                ))
        ).bodyToFlux(PayDto.class).collectList();
    }

    public Mono<Void>deletePaymentById(String token, Long id){
        return paymentServiceClient.delete().uri("/controller/pay/{id}", id)
                .header("Authorization", token)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        Mono.error(new ResponseStatusException(
                                clientResponse.statusCode(),
                                "Внешний сервис вернул ошибку"
                        ))
                )
                .bodyToMono(Void.class);
    }
}
