package com.example.demo.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.demo.model.AppUser;

import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AppUserRepository extends ReactiveCrudRepository<AppUser, Long> {

    Mono<AppUser> findByUsername(String username);

    @Query("""
        SELECT * 
        FROM app_users 
        WHERE role = 'COURIER'
        AND status = 'AVAILABLE'
    """)
    Flux<AppUser> findAvailableCouriers();

    @Query("""
        UPDATE app_users
        SET status = :status
        WHERE username = :username
    """)
    Mono<Void> updateStatus(String username, String status);
}
