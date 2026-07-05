package com.example.demo.service;

import com.example.demo.auth.CurrentUser;
import com.example.demo.dto.OrderItemDto;
import com.example.demo.dto.RequestCreateOrderItem;
import com.example.demo.model.OrderItem;
import com.example.demo.repository.OrderItemRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class OrderItemService {
    private final OrderItemRepository orderItemRepository;

    public Mono<OrderItemDto> createOrderItem(CurrentUser user, RequestCreateOrderItem req){
        var item = new OrderItem(null, req.name(), req.price(), req.quantity(), req.description(), user.username());
        return orderItemRepository.save(item).map(item2 -> {
            return new OrderItemDto(item2.getId(), item2.getName(), item2.getPrice(), item2.getQuantity(), item2.getDescription(), user.username());
        });
    }

    public Mono<Void> deleteOrderItem(CurrentUser user, Long id) {
        return orderItemRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Товар с ID " + id + " не найден"
                )))
                .filter(item -> item.getSupplier().equals(user.username()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "У вас нет прав на удаление этого товара"
                )))
                .flatMap(item -> {
                    log.info("Пользователь {} удаляет товар с ID: {}", user.username(), id);
                    return orderItemRepository.delete(item);
                });
    }

}
