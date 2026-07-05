package com.example.demo.service;

import com.example.demo.model.OrdersCouriers;
import com.example.demo.repository.OrdersCouriersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrderCourierService {

    private final OrdersCouriersRepository ordersCouriersRepository;
    private final DatabaseClient databaseClient;


     // Назначить курьера на заказ (m2m).
    @Transactional
    public Mono<Void> assignCourier(OrdersCouriers ordersCouriers) {
        return databaseClient.sql("""
                INSERT INTO orders_couriers (order_id, courier_username) 
                VALUES (:orderId, :courierUsername) 
                ON CONFLICT DO NOTHING
                """)
                .bind("orderId", ordersCouriers.getOrderId())
                .bind("courierUsername", ordersCouriers.getCourierUsername())
                .then();
    }


    // Снять курьера с заказа (Удалить связь m2m).
    @Transactional
    public Mono<Void> unassignCourier(Long orderId, String courierUsername) {
        return ordersCouriersRepository.deleteByOrderIdAndCourierUsername(orderId, courierUsername);
    }


    // Проверить, привязан ли конкретный курьер к конкретному заказу.
    public Mono<Boolean> isCourierAssigned(Long orderId, String courierUsername) {
        return ordersCouriersRepository.existsByOrderIdAndCourierUsername(orderId, courierUsername);
    }
}
