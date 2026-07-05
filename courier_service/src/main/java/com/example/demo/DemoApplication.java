package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.demo.config.JwtProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@EnableCaching
@Slf4j
public class DemoApplication {
//	@Bean
//	public CommandLineRunner test(ApplicationContext context) {
//		return args -> {
//			System.out.println(
//					"Flyway bean exists = " +
//							context.containsBean("flyway")
//			);
//			log.info("Flyway bean exists = " +
//					context.containsBean("flyway"));
//		};
//	}
//
//	@Bean
//	CommandLineRunner check(ApplicationContext context) {
//		return args -> {
//			System.out.println(
//					"DataSource bean = " +
//							context.containsBean("dataSource")
//			);
//
//			System.out.println(
//					"Flyway bean = " +
//							context.containsBean("flyway")
//			);
//		};
//	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
