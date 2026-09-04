package com.biadevcosta.scheduling.application.port;

import com.biadevcosta.scheduling.domain.Appointment;

/** Publishes appointment events to the history feed. Implemented with Kafka in infrastructure. */
public interface AppointmentEventPublisher {

    void publishCreated(Appointment appointment);

    void publishUpdated(Appointment appointment);
}
