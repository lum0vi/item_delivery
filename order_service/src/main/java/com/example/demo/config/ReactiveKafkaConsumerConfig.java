package com.example.demo.config;

import com.example.demo.dto.CourierDataOrder;
import com.example.demo.dto.CourierDataOrder;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class ReactiveKafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.group-id}")
    private String groupId;

    @Value("${app.kafka.topics.order:order-events}")
    private String topic_order_events ;

    @Bean
    public ReceiverOptions<String, CourierDataOrder> receiverOptions() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, CourierDataOrder.class.getName());
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.example.demo.dto");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return ReceiverOptions.<String, CourierDataOrder>create(props)
                .subscription(List.of("order-events"));
    }

    @Bean
    public KafkaReceiver<String, CourierDataOrder> kafkaReceiver(
            ReceiverOptions<String, CourierDataOrder> receiverOptions
    ) {
        return KafkaReceiver.create(receiverOptions);
    }
}
