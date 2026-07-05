package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "order_item")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {

    @Id
    @Column("id")
    private Long id;

    @Column("name")
    private String name;

    @Column("price")
    private Integer price;

    @Column("quantity")
    private Integer quantity;

    @Column("description")
    private String description;

    @Column("supplier")
    private String supplier;
}
