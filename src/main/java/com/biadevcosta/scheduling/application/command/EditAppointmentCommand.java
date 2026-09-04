package com.biadevcosta.scheduling.application.command;

import java.time.LocalDateTime;

/**
 * Input for {@code EditAppointmentUseCase}. {@code callerId} is the authenticated user id taken
 * from the JWT {@code sub} claim; ownership is checked against it inside the domain.
 */
public record EditAppointmentCommand(String appointmentId,
                                     LocalDateTime newScheduledAt,
                                     String newStatus,
                                     String callerId) {
}
