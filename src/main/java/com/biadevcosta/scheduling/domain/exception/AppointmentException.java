package com.biadevcosta.scheduling.domain.exception;

/** Base type for every business rule violation raised by the scheduling domain. */
public class AppointmentException extends RuntimeException {

    public AppointmentException(String message) {
        super(message);
    }
}
