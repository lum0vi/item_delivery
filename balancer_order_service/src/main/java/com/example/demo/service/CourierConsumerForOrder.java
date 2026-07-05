package com.example.demo.service;


import com.example.demo.dto.CourierDataOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.kafka.receiver.KafkaReceiver;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierConsumerForOrder {

    private final KafkaReceiver<String, CourierDataOrder> kafkaReceiver;

}
