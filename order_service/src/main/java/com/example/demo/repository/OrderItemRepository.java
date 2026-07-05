package com.example.demo.repository;

import com.example.demo.model.OrderItem;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface OrderItemRepository extends ReactiveCrudRepository<OrderItem, Long> {

    @Query("""
        SELECT oi.* FROM "order_item" oi
        JOIN "orders_order_item" ooi ON oi."id" = ooi."order_item_id"
        WHERE ooi."order_id" = :orderId
    """)
    Flux<OrderItem> findAllByOrderId(Long orderId);
}
