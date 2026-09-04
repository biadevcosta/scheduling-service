package com.biadevcosta.scheduling.application.command;

import java.time.LocalDateTime;

/** Input for {@code ScheduleAppointmentUseCase}. */
public record ScheduleAppointmentCommand(String patientId, String doctorId, LocalDateTime scheduledAt) {
}
