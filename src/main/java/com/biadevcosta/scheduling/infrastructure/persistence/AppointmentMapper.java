package com.biadevcosta.scheduling.infrastructure.persistence;

import com.biadevcosta.scheduling.domain.Appointment;
import com.biadevcosta.scheduling.domain.AppointmentStatus;

/** Translates between the {@link Appointment} domain object and its persistence row. */
final class AppointmentMapper {

    private AppointmentMapper() {
    }

    static AppointmentEntity toNewEntity(Appointment a) {
        AppointmentEntity e = new AppointmentEntity();
        copyInto(a, e);
        return e;
    }

    /** Copies the mutable domain state onto an existing row, keeping its {@code version}. */
    static void copyInto(Appointment a, AppointmentEntity e) {
        e.setId(a.id());
        e.setPatientId(a.patientId());
        e.setDoctorId(a.doctorId());
        e.setScheduledAt(a.scheduledAt());
        e.setStatus(a.status().name());
        e.setCreatedAt(a.createdAt());
    }

    static Appointment toDomain(AppointmentEntity e) {
        return Appointment.rehydrate(
                e.getId(), e.getPatientId(), e.getDoctorId(),
                e.getScheduledAt(), AppointmentStatus.valueOf(e.getStatus()), e.getCreatedAt());
    }
}
