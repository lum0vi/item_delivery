package com.example.demo.service;

import com.example.demo.dto.OrderDetailsForCourier;
import com.example.demo.dto.ResponseGetCourierOrder;
import com.example.demo.repository.OrdersCouriersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourierService {

    private static final String ACTION_ASSIGN_COURIER_CREATE = "CREATE";
    private static final String ACTION_ASSIGN_COURIER_GET = "GET";
    private static final String ACTION_ASSIGN_COURIER_DELETE = "DELETE";

    private final CourierAssignmentService courierAssignmentService;
    private final OrdersCouriersRepository ordersCouriersRepository;

    // обработка событий
    @Transactional
    public Mono<Void> process(OrderDetailsForCourier detailsForCourier) {
        if (detailsForCourier == null || detailsForCourier.action() == null) {
            return Mono.error(new IllegalArgumentException("Данные заказа или действие не могут быть null"));
        }

        switch (detailsForCourier.action()) {
            case ACTION_ASSIGN_COURIER_CREATE: {
                log.info("Kafka Event [CREATE]: Автоматический поиск и назначение курьера для заказа №{}",
                        detailsForCourier.order());

                return courierAssignmentService.assignCourier(detailsForCourier.order())
                        .doOnSuccess(relation -> log.info("Заказ №{} назначен на курьера {}. Код проверки: {}",
                                relation.getOrderId(), relation.getCourierUsername(), relation.getCode()))
                        .then();
            }

            case ACTION_ASSIGN_COURIER_DELETE: {
                log.info("Kafka Event [DELETE]: Запрос на отмену/завершение заказа №{} курьером {}",
                        detailsForCourier.order(), detailsForCourier.username());

                // Переводим заказ в статус 'done' и освобождаем курьера (ставим AVAILABLE)
                return courierAssignmentService.completeDelivery(
                        detailsForCourier.order(),
                        detailsForCourier.username(),
                        //detailsForCourier.ratingCourier() // Если код верификации передается в этом поле, иначе используйте нужный геттер
                        // так было сделано только потому, что эти данные текут только от проверенного клиента =>
                        // => я могу код не проверять (у клиента он есть) и не гонять просто так данные
                        64578123892349821L
                );
            }

            case ACTION_ASSIGN_COURIER_GET: {
                log.info("Kafka Event [GET]: Проверка привязки курьера {} к заказу №{}",
                        detailsForCourier.username(), detailsForCourier.order());

                return ordersCouriersRepository.existsByOrderIdAndCourierUsername(
                                detailsForCourier.order(),
                                detailsForCourier.username()
                        )
                        .doOnNext(isAssigned -> log.info("Результат проверки: Курьер {} привязан к заказу №{}: {}",
                                detailsForCourier.username(), detailsForCourier.order(), isAssigned))
                        .then();
            }

            default: {
                log.warn("Получено неизвестное действие в CourierService: {}", detailsForCourier.action());
                return Mono.empty();
            }
        }
    }


    // Получить данные активного заказа курьера. + кэш
//    @Cacheable(value = "getCourierOrder", key = "#username")
    public Mono<ResponseGetCourierOrder> getCourierOrder(String username, String status) {
        log.info("Запрос в БД (мимо кэша): Поиск заказа курьера {} со статусом {}", username, status);

        return ordersCouriersRepository.findByCourierUsernameAndStatus(username, status)
                .map(ordersCouriers -> new ResponseGetCourierOrder(
                        ordersCouriers.getOrderId(),
                        ordersCouriers.getStatus(),
                        0L // нельзя давать курьеру код
                ))
                .next();
    }

    public Mono<ResponseGetCourierOrder> getCourierOrderUser(String username, String status) {
        log.info("Запрос в БД (мимо кэша): Поиск заказа курьера {} со статусом {}", username, status);

        return ordersCouriersRepository.findByCourierUsernameAndStatus(username, status)
                .map(ordersCouriers -> new ResponseGetCourierOrder(
                        ordersCouriers.getOrderId(),
                        ordersCouriers.getStatus(),
                        ordersCouriers.getCode()
                ))
                .next();
    }


    public Flux<ResponseGetCourierOrder> getCourierOrders(String username){
        return ordersCouriersRepository.findAllByCourierUsername(username).map(res -> {
            if (res.getStatus().equals("start")){
                return new ResponseGetCourierOrder(res.getOrderId(), res.getStatus(), 0L);
            }
            return new ResponseGetCourierOrder(res.getOrderId(), res.getStatus(), res.getCode());
        });
    }
}
