package com.eolma.user.adapter.out.kafka;

import com.eolma.common.event.DomainEvent;
import com.eolma.common.kafka.EolmaKafkaProducer;
import com.eolma.user.application.port.out.EventPublisher;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final String TOPIC = "eolma.user.events";

    private final EolmaKafkaProducer kafkaProducer;

    public KafkaEventPublisher(EolmaKafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    public void publish(DomainEvent<?> event) {
        kafkaProducer.publish(TOPIC, event);
    }
}
