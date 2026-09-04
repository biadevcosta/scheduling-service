package com.biadevcosta.scheduling.domain;

import com.biadevcosta.scheduling.domain.exception.InvalidAppointmentException;
import com.biadevcosta.scheduling.domain.exception.NotAppointmentOwnerException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Appointment aggregate. Holds the business rules for scheduling and editing:
 * <ul>
 *   <li>{@code patientId} and {@code doctorId} are required;</li>
 *   <li>{@code scheduledAt} must be in the future at creation and at edit;</li>
 *   <li>only the owner doctor ({@code doctorId}) may edit the appointment.</li>
 * </ul>
 * This class has no framework dependency.
 */
public class Appointment {

    private final String id;
    private final String patientId;
    private String doctorId;
    private LocalDateTime scheduledAt;
    private AppointmentStatus status;
    private final LocalDateTime createdAt;

    private Appointment(String id, String patientId, String doctorId,
                        LocalDateTime scheduledAt, AppointmentStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.scheduledAt = scheduledAt;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** Creation factory: enforces the creation invariants. */
    public static Appointment schedule(String patientId, String doctorId, LocalDateTime scheduledAt) {
        if (isBlank(patientId) || isBlank(doctorId)) {
            throw new InvalidAppointmentException("patientId and doctorId are required");
        }
        if (scheduledAt == null || !scheduledAt.isAfter(LocalDateTime.now())) {
            throw new InvalidAppointmentException("scheduledAt must be in the future");
        }
        return new Appointment(UUID.randomUUID().toString(), patientId, doctorId,
                scheduledAt, AppointmentStatus.SCHEDULED, LocalDateTime.now());
    }

    /** Rebuilds an appointment from persistence without re-checking creation invariants. */
    public static Appointment rehydrate(String id, String patientId, String doctorId,
                                        LocalDateTime scheduledAt, AppointmentStatus status, LocalDateTime createdAt) {
        return new Appointment(id, patientId, doctorId, scheduledAt, status, createdAt);
    }

    /**
     * Applies an edit. The caller must be the owner doctor. A new date, when supplied, must be
     * in the future. A new status, when supplied, replaces the current one.
     */
    public void edit(String callerDoctorId, LocalDateTime newScheduledAt, AppointmentStatus newStatus) {
        if (!this.doctorId.equals(callerDoctorId)) {
            throw new NotAppointmentOwnerException();
        }
        if (newScheduledAt != null) {
            if (!newScheduledAt.isAfter(LocalDateTime.now())) {
                throw new InvalidAppointmentException("scheduledAt must be in the future");
            }
            this.scheduledAt = newScheduledAt;
        }
        if (newStatus != null) {
            this.status = newStatus;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public String id() {
        return id;
    }

    public String patientId() {
        return patientId;
    }

    public String doctorId() {
        return doctorId;
    }

    public LocalDateTime scheduledAt() {
        return scheduledAt;
    }

    public AppointmentStatus status() {
        return status;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }
}
