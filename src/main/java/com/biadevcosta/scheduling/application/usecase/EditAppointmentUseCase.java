package com.biadevcosta.scheduling.application.usecase;

import com.biadevcosta.scheduling.application.command.EditAppointmentCommand;
import com.biadevcosta.scheduling.application.port.AppointmentEventPublisher;
import com.biadevcosta.scheduling.application.port.AppointmentRepository;
import com.biadevcosta.scheduling.domain.Appointment;
import com.biadevcosta.scheduling.domain.AppointmentStatus;
import com.biadevcosta.scheduling.domain.exception.AppointmentNotFoundException;
import com.biadevcosta.scheduling.domain.exception.InvalidAppointmentException;

/**
 * Edits an existing appointment. Ownership ("only the owner doctor edits") is enforced by the
 * domain. Order is fixed: load, apply, persist, then publish.
 */
public class EditAppointmentUseCase {

    private final AppointmentRepository repository;
    private final AppointmentEventPublisher eventPublisher;

    public EditAppointmentUseCase(AppointmentRepository repository,
                                  AppointmentEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public Appointment execute(EditAppointmentCommand command) {
        Appointment appointment = repository.findById(command.appointmentId())
                .orElseThrow(() -> new AppointmentNotFoundException(command.appointmentId()));

        appointment.edit(command.callerId(), command.newScheduledAt(), parseStatus(command.newStatus()));

        Appointment saved = repository.save(appointment);
        eventPublisher.publishUpdated(saved);
        return saved;
    }

    private static AppointmentStatus parseStatus(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return AppointmentStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new InvalidAppointmentException("Unknown status: " + raw);
        }
    }
}
