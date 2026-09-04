package com.biadevcosta.scheduling.domain.exception;

/** Raised when an appointment id cannot be resolved. */
public class AppointmentNotFoundException extends AppointmentException {

    public AppointmentNotFoundException(String id) {
        super("Appointment not found: " + id);
    }
}
