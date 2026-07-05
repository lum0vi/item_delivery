package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.demo.config.JwtProperties;

import java.util.Random;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class DemoApplication {
	public static String nameService = "order_service_main" ;
	public static Random random = new Random();

	// доделать courier сервис с назначением и передачей данных о геолокации курьера и ошибках с ним + jwt с ним
	// сделать чат с курьером и менеджерами
	// сделать системы платежей и скидок
	// сделать вход по вк и яндекс
	// сделать сбор статистики

	static {
		DemoApplication.nameService += random.nextInt();
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
