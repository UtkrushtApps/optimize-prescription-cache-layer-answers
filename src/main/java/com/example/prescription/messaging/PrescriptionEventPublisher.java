package com.example.prescription.messaging;

import com.example.prescription.dto.PrescriptionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes prescription change events to Kafka.
 * Failures are logged but do not cause the main request to fail.
 */
@Component
public class PrescriptionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionEventPublisher.class);

    private static final String TOPIC = "prescription-updates";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PrescriptionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPrescriptionUpdated(PrescriptionDto dto) {
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(dto.getId()), dto);
            log.debug("Published prescription update event for id={}", dto.getId());
        } catch (Exception ex) {
            log.error("Failed to publish prescription update event for id={}", dto.getId(), ex);
        }
    }
}
