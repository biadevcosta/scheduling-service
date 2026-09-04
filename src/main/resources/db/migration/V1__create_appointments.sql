CREATE TABLE appointments (
    id           VARCHAR(36) NOT NULL PRIMARY KEY,
    version      BIGINT      NULL,
    patient_id   VARCHAR(36) NOT NULL,
    doctor_id    VARCHAR(36) NOT NULL,
    scheduled_at DATETIME    NOT NULL,
    status       VARCHAR(20) NOT NULL,
    created_at   DATETIME    NOT NULL
);

CREATE INDEX idx_appointments_patient ON appointments (patient_id);
CREATE INDEX idx_appointments_doctor ON appointments (doctor_id);
