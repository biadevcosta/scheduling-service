package com.biadevcosta.scheduling.application.port;

import com.biadevcosta.scheduling.domain.Appointment;

/** Publishes the reminder task. Implemented with RabbitMQ in infrastructure. */
public interface ReminderPublisher {

    void publishReminder(Appointment appointment);
}
