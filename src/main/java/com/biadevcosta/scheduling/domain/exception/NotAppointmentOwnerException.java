package com.biadevcosta.scheduling.domain.exception;

/** Raised when a doctor other than the owner tries to edit an appointment. */
public class NotAppointmentOwnerException extends AppointmentException {

    public NotAppointmentOwnerException() {
        super("Only the owner doctor can edit this appointment");
    }
}
