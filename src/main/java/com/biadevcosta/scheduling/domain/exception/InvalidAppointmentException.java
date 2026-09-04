package com.biadevcosta.scheduling.domain.exception;

/** Raised when an appointment breaks a creation/edit invariant (missing ids, past date, ...). */
public class InvalidAppointmentException extends AppointmentException {

    public InvalidAppointmentException(String reason) {
        super(reason);
    }
}
