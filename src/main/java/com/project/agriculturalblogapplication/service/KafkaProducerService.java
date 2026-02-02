package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.events.ApplicationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, ApplicationEvent> kafkaTemplate;

    public void publishEvent(String topic, ApplicationEvent event) {
        log.info("Publishing event to topic {}: {}", topic, event.getEventType());
        kafkaTemplate.send(topic, event.getEventId(), event);
    }
}
