package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "orders_couriers")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrdersCouriers {

    @Column("order_id")
    private Long orderId;

    @Column("courier_username")
    private String courierUsername;

    @Column("status")
    private String status;

    @Column("code")
    private Long code;
}
