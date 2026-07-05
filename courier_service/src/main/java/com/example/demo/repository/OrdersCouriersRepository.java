package com.example.demo.repository;

import com.example.demo.model.OrdersCouriers;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrdersCouriersRepository extends ReactiveCrudRepository<OrdersCouriers, Void> {

     //Получить все ID заказов, закрепленных за конкретным курьером.
    @Query("""
        SELECT order_id 
        FROM orders_couriers 
        WHERE courier_username = :courierUsername
    """)
    Flux<Long> findOrderIdsByCourierUsername(String courierUsername);


     // Получить ID заказов курьера, отфильтрованные по конкретному статусу.
    @Query("""
        SELECT order_id 
        FROM orders_couriers 
        WHERE courier_username = :courierUsername 
          AND status = :status
    """)
    Flux<Long> findOrderIdsByCourierUsernameAndStatus(String courierUsername, String status);


     // Создать новую связь m2m с указанием начального статуса и проверочного кода.
    @Query("""
        INSERT INTO orders_couriers (order_id, courier_username, status, code)
        VALUES (:orderId, :courierUsername, :status, :code)
    """)
    Mono<Void> insert(Long orderId, String courierUsername, String status, Long code);


    //Найти  запись назначения для проверки секретного кода или маппинга в DTO.
    @Query("""
        SELECT order_id, courier_username, status, code 
        FROM orders_couriers 
        WHERE order_id = :orderId 
          AND courier_username = :courierUsername
    """)
    Mono<OrdersCouriers> findByOrderIdAndCourierUsername(Long orderId, String courierUsername);

    //Найти запись по курьеру и статусу (getCourierOrder в CourierService).
    @Query("""
        SELECT order_id, courier_username, status, code 
        FROM orders_couriers 
        WHERE courier_username = :courierUsername 
          AND status = :status
    """)
    Flux<OrdersCouriers> findByCourierUsernameAndStatus(String courierUsername, String status);

    //Обновить статус доставки для конкретного заказа и курьера.
    @Query("""
        UPDATE orders_couriers 
        SET status = :status 
        WHERE order_id = :orderId 
          AND courier_username = :courierUsername
    """)
    Mono<Void> updateStatus(Long orderId, String courierUsername, String status);

    //Полностью удалить связь между заказом и курьером.
    @Query("""
        DELETE FROM orders_couriers 
        WHERE order_id = :orderId 
          AND courier_username = :courierUsername
    """)
    Mono<Void> deleteByOrderIdAndCourierUsername(Long orderId, String courierUsername);


    //Проверить существование связи между заказом и курьером.
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM orders_couriers 
            WHERE order_id = :orderId 
              AND courier_username = :courierUsername
        )
    """)
    Mono<Boolean> existsByOrderIdAndCourierUsername(Long orderId, String courierUsername);

    //Все заказы курьера
    @Query("""
        SELECT order_id, courier_username, status, code 
        FROM orders_couriers 
        WHERE courier_username = :courierUsername
    """)
    Flux<OrdersCouriers> findAllByCourierUsername(String courierUsername);


    @Query("""
        UPDATE app_users
        SET status = 'BUSY'
        WHERE username = (
            SELECT username
            FROM app_users
            WHERE role = 'COURIER'
              AND status = 'AVAILABLE'
            ORDER BY random()
            LIMIT 1
            FOR UPDATE SKIP LOCKED
        )
        RETURNING username
    """)
    Mono<String> lockRandomAvailableCourier();
}
