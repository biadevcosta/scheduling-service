package com.biadevcosta.scheduling.application.usecase;

import com.biadevcosta.scheduling.application.command.ScheduleAppointmentCommand;
import com.biadevcosta.scheduling.application.port.AppointmentEventPublisher;
import com.biadevcosta.scheduling.application.port.AppointmentRepository;
import com.biadevcosta.scheduling.application.port.ReminderPublisher;
import com.biadevcosta.scheduling.domain.Appointment;

/**
 * Registers a new appointment. Order is fixed: validate, persist (source of truth), then publish.
 * Nothing is published before the row is committed.
 */
public class ScheduleAppointmentUseCase {

    private final AppointmentRepository repository;
    private final ReminderPublisher reminderPublisher;
    private final AppointmentEventPublisher eventPublisher;

    public ScheduleAppointmentUseCase(AppointmentRepository repository,
                                      ReminderPublisher reminderPublisher,
                                      AppointmentEventPublisher eventPublisher) {
        this.repository = repository;
        this.reminderPublisher = reminderPublisher;
        this.eventPublisher = eventPublisher;
    }

    public Appointment execute(ScheduleAppointmentCommand command) {
        Appointment appointment = Appointment.schedule(
                command.patientId(), command.doctorId(), command.scheduledAt());

        Appointment saved = repository.save(appointment);   // 1) persist
        reminderPublisher.publishReminder(saved);           // 2) RabbitMQ reminder
        eventPublisher.publishCreated(saved);               // 3) Kafka event
        return saved;
    }
}
