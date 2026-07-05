package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDate;

@Table(name = "payments")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Payment {

    @Id
    @Column("id")
    private Long id;

    @Column("user_username")
    private String user;

    @Column("date_payment")
    private LocalDate datePayment;

    @Column("order_id")
    private Long orderId;

    @Column("payment_method")
    private String paymentMethod;
}
