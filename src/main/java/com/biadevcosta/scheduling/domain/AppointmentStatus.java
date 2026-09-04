package com.biadevcosta.scheduling.domain;

/** Lifecycle of an appointment. A newly created appointment starts as {@link #SCHEDULED}. */
public enum AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
}
