package com.example.demo;

import com.example.demo.auth.CurrentUser;
import com.example.demo.dto.*;
import com.example.demo.middleware.JwtAuthenticationWebFilter;
import com.example.demo.service.OrderItemService;
import com.example.demo.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/controller")
@RequiredArgsConstructor
@Tag(name="order_controller")
public class OrderController {

    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final KafkaSender<String, OrderDetailsForCourier>kafkaSender;
    private long max_rating = 100L;
    private final String actionAssignCourierCreate = "CREATE";
    private final String actionAssignCourierGet = "GET";
    private final String actionAssignCourierDelete = "DELETE";

    @Value("${app.kafka.topics.order:order-events}")
    private String order_topic_events;

    @PostMapping("/order")
    public Mono<OrderDto> CreateOrder(ServerWebExchange exchange, @RequestBody RequestCreateOrderDto req){
        return orderService.createOrder(req);
    }

    @GetMapping("/order/{id}")
    public Mono<OrderDto> GetOrder(ServerWebExchange exchange, @PathVariable("id") Long id){
        return orderService.getOrder(currentUser(exchange), id);
    }

    @GetMapping("/order_user")
    public Mono<List<OrderDto>> GetOrderUser(ServerWebExchange exchange){
        return orderService.getOrderUser(currentUser(exchange));
    }

    @PostMapping("/order_item")
    public Mono<OrderItemDto> CreateOrderItem(ServerWebExchange exchange, @RequestBody RequestCreateOrderItem req){
        return orderItemService.createOrderItem(currentUser(exchange), req);
    }

    @DeleteMapping("/order_item/{id}")
    public Mono<Void> DeleteOrderItem(ServerWebExchange exchange, @PathVariable("id") Long id){
        return orderItemService.deleteOrderItem(currentUser(exchange), id);
    }

    @PostMapping("/{id}/pay")
    public Mono<PayDto> PostPaymentOrder(ServerWebExchange exchange, @PathVariable("id") Long id, @RequestBody OrderPymentRequestDto req){
        log.info(req.order().toString(), id.toString());
        String authorization = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);
        return orderService.paymentOrder(currentUser(exchange), id, req,authorization);
    }

    @GetMapping("/pay")
    public Mono<List<PayDto>> GetPaymentOrder(ServerWebExchange exchange){
        return orderService.getPaymentOrder(exchange.getRequest().getHeaders().getFirst("Authorization"));
    }

    @DeleteMapping("/pay/{id}")
    public Mono<Void> DeletePaymentById(ServerWebExchange exchange, @PathVariable("id") Long id){
        return orderService.deletePaymentById(exchange.getRequest().getHeaders().getFirst("Authorization"), id);
    }

    @PostMapping("/assign_courier")
    public Mono<Void> CreateAssignCourier(ServerWebExchange exchange, @RequestBody RequestCreateAssignCourier req){
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        return orderService.getOrder(currentUser(exchange), req.order_id()).flatMap(orderDto -> {
            return orderService.getPaymentOrderId(token, orderDto.id()).flatMap(payDto -> {
                OrderDetailsForCourier payload = new OrderDetailsForCourier(
                        orderDto.id(),
                        //null, // Заполни payment
                        orderDto.address(),
                        orderDto.user(),
                        (long) (Math.random() * max_rating),
                        actionAssignCourierCreate
                );
                SenderRecord<String, OrderDetailsForCourier, String> record = SenderRecord.create(
                        new ProducerRecord<>(order_topic_events, String.valueOf(orderDto.id()), payload),
                        String.valueOf(orderDto.id()) // Ссылка для корреляции (correlation metadata)
                );
                return kafkaSender.send(Mono.just(record)).next();
            });
        }).onErrorResume(ResponseStatusException.class, ex -> {
                    return Mono.error(new ResponseStatusException(
                            ex.getStatusCode(),
                            "Не удалось назначить курьера: " + ex.getReason()
                    ));
                })
                .then();
    }

//    @GetMapping("/assign_courier/{id}")
//    public Mono<Void> GetAssignCourier(ServerWebExchange exchange, Long id_order){
//        return orderService.getOrder(currentUser(exchange), id_order).flatMap(orderDto -> {
//            OrderDetailsForCourier payload = new OrderDetailsForCourier(
//                    orderDto.id(),
//                    //null, // Заполни payment
//                    orderDto.address(),
//                    orderDto.user(),
//                    (long) (Math.random() * max_rating),
//                    actionAssignCourierGet
//            );
//            SenderRecord<String, OrderDetailsForCourier, String> record = SenderRecord.create(
//                    new ProducerRecord<>(order_topic_events, String.valueOf(orderDto.id()), payload),
//                    String.valueOf(orderDto.id()) // Ссылка для корреляции (correlation metadata)
//            );
//            return kafkaSender.send(Mono.just(record)).next();
//        }).then();
//    }

    @DeleteMapping("/assign_courier/{id}/cancel")
    public Mono<Void> DeleteAssignCourier(ServerWebExchange exchange,
                                          @PathVariable("id") Long id_order,
                                          @RequestBody AssignCourierDeleteRequest req){
        return orderService.getOrder(currentUser(exchange), id_order).flatMap(orderDto -> {
            OrderDetailsForCourier payload = new OrderDetailsForCourier(
                    orderDto.id(),
                    //null, // Заполни payment
                    orderDto.address(),
                    req.courier_name(),
                    (long) (Math.random() * max_rating),
                    actionAssignCourierDelete
            );
            SenderRecord<String, OrderDetailsForCourier, String> record = SenderRecord.create(
                    new ProducerRecord<>(order_topic_events, String.valueOf(orderDto.id()), payload),
                    String.valueOf(orderDto.id())
            );
            return kafkaSender.send(Mono.just(record)).next();
        }).then();
    }

    private CurrentUser currentUser(ServerWebExchange exchange) {
        CurrentUser currentUser = exchange.getAttribute(JwtAuthenticationWebFilter.CURRENT_USER_ATTRIBUTE);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user is missing");
        }
        return currentUser;
    }
}
