package com.biadevcosta.scheduling.infrastructure.messaging.kafka;

import com.biadevcosta.scheduling.application.port.AppointmentEventPublisher;
import com.biadevcosta.scheduling.domain.Appointment;
import com.biadevcosta.scheduling.infrastructure.messaging.AppointmentEventMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * {@link AppointmentEventPublisher} adapter for Kafka. The message key is the {@code patientId}, so
 * every event for a patient lands on the same partition and stays ordered.
 */
@Component
public class KafkaAppointmentEventPublisher implements AppointmentEventPublisher {

    private final KafkaTemplate<String, Object> template;
    private final String topic;

    public KafkaAppointmentEventPublisher(KafkaTemplate<String, Object> template,
                                          @Value("${app.kafka.topic}") String topic) {
        this.template = template;
        this.topic = topic;
    }

    @Override
    public void publishCreated(Appointment appointment) {
        send(AppointmentEventMessage.CREATED, appointment);
    }

    @Override
    public void publishUpdated(Appointment appointment) {
        send(AppointmentEventMessage.UPDATED, appointment);
    }

    private void send(String type, Appointment a) {
        AppointmentEventMessage event = new AppointmentEventMessage(
                type, UUID.randomUUID().toString(),
                a.id(), a.patientId(), a.doctorId(), a.scheduledAt(), a.status().name());
        template.send(topic, a.patientId(), event);
    }
}
