package com.biadevcosta.scheduling.infrastructure.messaging;

import java.time.LocalDateTime;

/** RabbitMQ payload. Carries ids only; notification-service resolves the patient's contact. */
public record AppointmentReminderMessage(String appointmentId, String patientId, LocalDateTime scheduledAt) {
}
