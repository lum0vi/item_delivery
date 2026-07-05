# Agent.md — Order Management System (Reactive)

## Общее описание проекта
Проект представляет собой **реактивную систему управления заказами** для интернет-магазина, построенную на **Spring Boot 3** с использованием **WebFlux**, **R2DBC**, **Apache Kafka**, **Redis** и **JWT-аутентификации**.  
Архитектура — модульный монолит (с перспективой распиливания на микросервисы). Кодовая база демонстрирует Middle-уровень владения реактивным программированием, безопасностью, работой с базой данных и асинхронной обработкой событий.

## Технологический стек
- **Язык:** Java 21
- **Сборщик:** Maven (pom.xml), возможен Gradle
- **Фреймворк:** Spring Boot 3.3.x
- **Реактивный веб:** Spring WebFlux (стартер `spring-boot-starter-webflux`)
- **Безопасность:** Spring Security (реактивная), JWT (библиотека jjwt 0.12.5)
- **База данных:** PostgreSQL, доступ через **R2DBC** (стартер `spring-boot-starter-data-r2dbc`, драйвер `r2dbc-postgresql`)
- **Миграции:** Flyway (через JDBC, стартер `flyway-core`)
- **Redis:** Reactive Redis (стартер `spring-boot-starter-data-redis-reactive`)
- **Брокер сообщений:** Apache Kafka (стартер `spring-kafka`)
- **Валидация:** `spring-boot-starter-validation`
- **Мониторинг:** Spring Boot Actuator
- **Утилиты:** Lombok, MapStruct (опционально)
- **Тестирование:** JUnit 5, Mockito, Reactor Test, Spring Security Test (в планах — Testcontainers)

## Архитектура и структура пакетов
```
com.shop
├── ShopApplication.java
├── config
│   ├── SecurityConfig.java
│   └── KafkaConfig.java (опционально)
├── security
│   ├── JwtTokenProvider.java
│   ├── JwtReactiveFilter.java (WebFilter)
│   ├── ReactiveRefreshTokenStorage.java
│   └── ReactiveUserDetailsServiceImpl.java
├── user
│   ├── User.java (реализует UserDetails)
│   ├── Role.java
│   ├── UserRepository.java (ReactiveCrudRepository)
│   ├── RoleRepository.java
│   ├── UserRoleRepository.java (для связи many-to-many, опционально)
│   └── UserService.java
├── product
│   ├── Product.java
│   ├── Category.java
│   ├── ProductRepository.java
│   ├── ProductService.java
│   └── ProductController.java
├── order
│   ├── Order.java
│   ├── OrderItem.java
│   ├── OrderRepository.java
│   ├── OrderService.java
│   └── OrderController.java
├── cart
│   ├── Cart.java
│   ├── CartItem.java
│   ├── CartRepository.java
│   └── CartService.java
├── payment
│   ├── Payment.java
│   ├── PaymentRepository.java
│   ├── PaymentService.java
│   └── PaymentController.java
├── notification
│   └── NotificationService.java (слушает Kafka)
├── kafka
│   ├── OrderEventProducer.java
│   ├── OrderEventConsumer.java
│   └── OrderEvent.java (DTO)
├── dto
│   ├── AuthRequest.java, AuthResponse.java, ...
│   └── ...
├── exception
│   ├── GlobalExceptionHandler.java
│   └── ...
└── ...
```
*Примечание:* Связи `many-to-many` (User ↔ Role) реализуются через отдельную таблицу `user_roles`, загрузка ролей происходит вручную с помощью `DatabaseClient` или отдельного репозитория.

## Конфигурация приложения (`application.yml`)
Основные параметры (полный файл приведён ранее):
```yaml
server.port: 8080
jwt.access-token.secret: <секрет>
jwt.access-token.expiration-ms: 900000
jwt.refresh-token.expiration-ms: 604800000

spring.r2dbc:
  url: r2dbc:postgresql://localhost:5432/order_management
  username/password: ...
  pool: ...

spring.flyway:
  url: jdbc:postgresql://...  # Flyway требует JDBC URL

spring.data.redis: host, port, password, timeout

spring.kafka:
  bootstrap-servers: ...
  consumer: ...
  producer: ...
```

## База данных и миграции
Используется PostgreSQL. Схема управляется **Flyway** (файлы в `src/main/resources/db/migration`).  
Главные таблицы: `users`, `roles`, `user_roles`, `categories`, `products`, `cart`, `cart_item`, `addresses`, `orders`, `order_items`, `payments`, `notifications`, `audit_log`.  
Все первичные ключи — UUID, генерируются через `gen_random_uuid()`.

**R2DBC не поддерживает отношения, поэтому внешние ключи описаны только на уровне БД, в сущностях связи не мапятся.** Для загрузки связанных данных используются ручные запросы (`DatabaseClient`) или отдельные репозитории.

Пример миграции (V1__init_schema.sql):
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE roles (...);
INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN'), ('ROLE_VENDOR');
CREATE TABLE user_roles (...);
-- далее другие таблицы
```

## Аутентификация и авторизация

### JWT
- **Access токен**: подписан HMAC-SHA256, содержит `sub` (email пользователя), `roles` (строка через запятую), время жизни 15 минут.
- **Refresh токен**: также подписан, содержит только `sub`, время жизни 7 дней. Хранится в Redis с ключом `refresh_token:{email}`.
- **Генерация/проверка** — класс `JwtTokenProvider`.

### Безопасность в WebFlux
- Кастомный фильтр `JwtReactiveFilter` реализует `WebFilter`. Извлекает токен из заголовка `Authorization: Bearer ...`, валидирует, загружает пользователя через `ReactiveUserDetailsService` и помещает аутентификацию в реактивный контекст через `ReactiveSecurityContextHolder.withAuthentication(auth)`.
- `SecurityConfig` определяет `SecurityWebFilterChain`, разрешает доступ к эндпоинтам на основе ролей (`hasRole`, `hasAnyRole`). Включена аннотационная безопасность (`@EnableReactiveMethodSecurity`).
- Пароли хешируются BCrypt.
- `ReactiveAuthenticationManager` реализован через `UserDetailsRepositoryReactiveAuthenticationManager`.

### Роли и права
- **ROLE_USER** — базовый покупатель. Может просматривать товары, оформлять заказы.
- **ROLE_VENDOR** — продавец. Может создавать, редактировать и удалять свои товары, просматривать статистику по своим товарам.
- **ROLE_ADMIN** — администратор. Полный доступ, включая управление пользователями, всеми товарами и заказами.
- Пользователь может иметь несколько ролей одновременно (например, быть и покупателем, и продавцом).

### Refresh токенов
`ReactiveRefreshTokenStorage` использует `ReactiveStringRedisTemplate`. При логине пара токенов сохраняется в Redis. При обновлении:
1. Проверяется валидность refresh-токена.
2. Извлекается email.
3. Сравнивается с сохранённым в Redis значением.
4. При успехе старый токен удаляется, генерируется новая пара, сохраняется в Redis.
   Таким образом реализован rotation и защита от кражи.

## Реактивный доступ к данным (R2DBC)
- Сущности аннотируются `@Table`, поля — `@Column`. Первичный ключ помечается `@Id`.
- Репозитории расширяют `ReactiveCrudRepository` (или `ReactiveSortingRepository`).
- Связи не поддерживаются, поэтому для объединения таблиц используется `DatabaseClient` с SQL-запросами. Например, загрузка пользователя с ролями делается через JOIN и группировку строк.
- Транзакции управляются через `TransactionOperator` или `@Transactional` в сервисах (через `R2dbcTransactionManager`).
- В сервисах все методы возвращают `Mono`/`Flux`.

## Kafka
- Используется для событийной модели: при создании заказа, изменении его статуса отправляется событие `OrderEvent` в топик `order-events`.
- `OrderEventProducer` отправляет сообщения через `KafkaTemplate`.
- `OrderEventConsumer` слушает топик и инициирует уведомления (email/логирование).
- Настройки Kafka в `application.yml`: сериализация/десериализация JSON с помощью `JsonSerializer`/`JsonDeserializer`, trusted packages — `com.shop.kafka.event`.

## Дополнительные модули
- **Корзина** — пока не полностью описана, но предполагается хранение в Redis для гостей и в БД для авторизованных.
- **Уведомления** — слушатель Kafka для отправки email (заглушка) и WebSocket (для онлайн-оповещений) — при необходимости будет добавлен.

## Тестирование
- Unit-тесты: JUnit 5 + Mockito.
- Интеграционные тесты: `WebTestClient` для контроллеров, `Testcontainers` для PostgreSQL и Redis (опционально).
- Тесты безопасности: `@WithMockUser`, `@WithUserDetails`.

## Запуск (Docker Compose)
Для локальной разработки используется `docker-compose.yml` с сервисами:
- `postgres` (16-alpine)
- `redis` (7-alpine)
- `zookeeper` и `kafka` (confluentinc images)

## Стиль кода и соглашения
- **Lombok** активно используется (`@Data`, `@Builder`, `@RequiredArgsConstructor`).
- DTO создаются через records (`AuthRequest`, `AuthResponse`, `RefreshRequest` и т.д.).
- Исключения обрабатываются глобальным `@RestControllerAdvice`.
- Имена пакетов соответствуют доменным модулям (DDD-lite).

## Текущее состояние (на момент написания Agent.md)
- ✅ Аутентификация и авторизация с JWT и refresh-токенами (реактивная).
- ✅ Базовые миграции и настройка R2DBC.
- ✅ Kafka продюсер/консьюмер.
- ✅ CRUD для товаров с учетом ролей.
- 🔲 Полная реализация заказов, корзины, оплаты, уведомлений.
- 🔲 Тесты (в процессе).
- 🔲 Документация OpenAPI (планируется).

Этот файл предоставляет контекст для AI-агента, помогающего в доработке и поддержке проекта. Все классы, упомянутые здесь, должны быть реализованы в соответствии с описанной архитектурой.