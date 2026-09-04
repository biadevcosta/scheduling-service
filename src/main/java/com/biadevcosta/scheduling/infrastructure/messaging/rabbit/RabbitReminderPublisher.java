package com.biadevcosta.scheduling.infrastructure.messaging.rabbit;

import com.biadevcosta.scheduling.application.port.ReminderPublisher;
import com.biadevcosta.scheduling.domain.Appointment;
import com.biadevcosta.scheduling.infrastructure.messaging.AppointmentReminderMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** {@link ReminderPublisher} adapter that sends the reminder task to RabbitMQ. */
@Component
public class RabbitReminderPublisher implements ReminderPublisher {

    private final RabbitTemplate template;
    private final String exchange;
    private final String routingKey;

    public RabbitReminderPublisher(RabbitTemplate template,
                                   @Value("${app.rabbit.exchange}") String exchange,
                                   @Value("${app.rabbit.routing-key}") String routingKey) {
        this.template = template;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void publishReminder(Appointment appointment) {
        template.convertAndSend(exchange, routingKey, new AppointmentReminderMessage(
                appointment.id(), appointment.patientId(), appointment.scheduledAt()));
    }
}
