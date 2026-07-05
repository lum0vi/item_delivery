package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "orders_order_item")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrdersOrderItem {

    @Column("order_id")
    private Long orderId;

    @Column("order_item_id")
    private Long orderItemId;
}
