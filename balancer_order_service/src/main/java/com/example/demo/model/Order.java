package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDate;

@Table(name = "orders")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    @Column("id")
    private Long id;

    @Column("user_username")
    private String user;

    @Column("discount")
    private Integer discount;

    @Column("total_sum")
    private Integer totalSum;

    @Column("address")
    private String address;

    @Column("order_date")
    private LocalDate orderDate;
}
//доделать с колонкой даты