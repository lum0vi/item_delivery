package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import org.springframework.data.domain.Persistable;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;


@Table("categories")
public class Сategories implements Persistable<UUID> {
    @Id
    private UUID id;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("arent_id")
    private UUID arent_id;

    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setArent_id(UUID ar_id) {
        this.arent_id = ar_id;
    }

    public UUID getArent_id() {
        return arent_id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean isNew() {
        // Возвращаем true, чтобы R2DBC всегда делал INSERT,
        // а не пытался искать запись для UPDATE
        return true;
    }
}