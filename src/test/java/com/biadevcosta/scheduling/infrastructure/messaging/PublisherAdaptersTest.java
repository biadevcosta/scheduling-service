package com.biadevcosta.scheduling.infrastructure.messaging;

import com.biadevcosta.scheduling.domain.Appointment;
import com.biadevcosta.scheduling.domain.AppointmentStatus;
import com.biadevcosta.scheduling.infrastructure.messaging.kafka.KafkaAppointmentEventPublisher;
import com.biadevcosta.scheduling.infrastructure.messaging.rabbit.RabbitReminderPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PublisherAdaptersTest {

    @Mock
    RabbitTemplate rabbitTemplate;
    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    private static Appointment appointment() {
        return Appointment.rehydrate("apt-1", "pat-1", "doc-1",
                LocalDateTime.of(2030, 5, 1, 8, 0), AppointmentStatus.SCHEDULED,
                LocalDateTime.of(2030, 4, 1, 8, 0));
    }

    @Test
    void rabbitPublisher_sendsReminderWithIdsOnly() {
        var publisher = new RabbitReminderPublisher(rabbitTemplate, "ex", "rk");

        publisher.publishReminder(appointment());

        ArgumentCaptor<AppointmentReminderMessage> captor =
                ArgumentCaptor.forClass(AppointmentReminderMessage.class);
        verify(rabbitTemplate).convertAndSend(eq("ex"), eq("rk"), captor.capture());
        assertThat(captor.getValue().appointmentId()).isEqualTo("apt-1");
        assertThat(captor.getValue().patientId()).isEqualTo("pat-1");
    }

    @Test
    void kafkaPublisher_created_keysByPatientId() {
        var publisher = new KafkaAppointmentEventPublisher(kafkaTemplate, "appointment-events");

        publisher.publishCreated(appointment());

        ArgumentCaptor<AppointmentEventMessage> captor =
                ArgumentCaptor.forClass(AppointmentEventMessage.class);
        verify(kafkaTemplate).send(eq("appointment-events"), eq("pat-1"), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(AppointmentEventMessage.CREATED);
        assertThat(captor.getValue().eventId()).isNotBlank();
        assertThat(captor.getValue().doctorId()).isEqualTo("doc-1");
    }

    @Test
    void kafkaPublisher_updated_usesUpdatedType() {
        var publisher = new KafkaAppointmentEventPublisher(kafkaTemplate, "appointment-events");

        publisher.publishUpdated(appointment());

        ArgumentCaptor<AppointmentEventMessage> captor =
                ArgumentCaptor.forClass(AppointmentEventMessage.class);
        verify(kafkaTemplate).send(eq("appointment-events"), eq("pat-1"), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(AppointmentEventMessage.UPDATED);
    }
}
