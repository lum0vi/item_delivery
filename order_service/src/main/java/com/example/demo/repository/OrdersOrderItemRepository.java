package com.example.demo.repository;

import com.example.demo.model.OrdersOrderItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdersOrderItemRepository extends ReactiveCrudRepository<OrdersOrderItem, Long> {
}