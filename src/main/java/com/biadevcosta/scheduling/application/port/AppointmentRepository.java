package com.biadevcosta.scheduling.application.port;

import com.biadevcosta.scheduling.domain.Appointment;

import java.util.Optional;

/** Persistence port for the appointment aggregate. Implemented in infrastructure. */
public interface AppointmentRepository {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(String id);
}
