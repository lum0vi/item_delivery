//package com.example.demo.config;
//
//import org.flywaydb.core.Flyway;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class FlywayConfig {
//
//    @Bean(initMethod = "migrate")
//    Flyway flyway() {
//        return Flyway.configure()
//                .dataSource(
//                        "jdbc:postgresql://localhost:5432/demo_db",
//                        "postgres",
//                        "postgres"
//                )
//                .load();
//    }
//}