package com.biadevcosta.scheduling.infrastructure.messaging;

import java.time.LocalDateTime;

/**
 * Kafka payload for topic {@code appointment-events}. {@code eventId} is unique per publication so
 * the consumer (history-service) can dedupe redeliveries.
 */
public record AppointmentEventMessage(String type,
                                      String eventId,
                                      String appointmentId,
                                      String patientId,
                                      String doctorId,
                                      LocalDateTime scheduledAt,
                                      String status) {

    public static final String CREATED = "AppointmentCreated";
    public static final String UPDATED = "AppointmentUpdated";
}
