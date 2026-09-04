package com.biadevcosta.scheduling.domain;

import com.biadevcosta.scheduling.domain.exception.InvalidAppointmentException;
import com.biadevcosta.scheduling.domain.exception.NotAppointmentOwnerException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentTest {

    private static final LocalDateTime FUTURE = LocalDateTime.now().plusDays(1);

    @Test
    void schedule_startsAsScheduled() {
        Appointment a = Appointment.schedule("pat-1", "doc-1", FUTURE);

        assertThat(a.id()).isNotBlank();
        assertThat(a.patientId()).isEqualTo("pat-1");
        assertThat(a.doctorId()).isEqualTo("doc-1");
        assertThat(a.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(a.createdAt()).isNotNull();
    }

    @Test
    void schedule_rejectsMissingIds() {
        assertThatThrownBy(() -> Appointment.schedule(null, "doc-1", FUTURE))
                .isInstanceOf(InvalidAppointmentException.class);
        assertThatThrownBy(() -> Appointment.schedule("pat-1", " ", FUTURE))
                .isInstanceOf(InvalidAppointmentException.class);
    }

    @Test
    void schedule_rejectsPastOrNullDate() {
        assertThatThrownBy(() -> Appointment.schedule("pat-1", "doc-1", LocalDateTime.now().minusMinutes(1)))
                .isInstanceOf(InvalidAppointmentException.class)
                .hasMessageContaining("future");
        assertThatThrownBy(() -> Appointment.schedule("pat-1", "doc-1", null))
                .isInstanceOf(InvalidAppointmentException.class);
    }

    @Test
    void edit_onlyOwnerDoctorMayEdit() {
        Appointment a = Appointment.schedule("pat-1", "doc-1", FUTURE);

        assertThatThrownBy(() -> a.edit("doc-999", FUTURE.plusDays(1), null))
                .isInstanceOf(NotAppointmentOwnerException.class);
    }

    @Test
    void edit_ownerChangesDateAndStatus() {
        Appointment a = Appointment.schedule("pat-1", "doc-1", FUTURE);
        LocalDateTime newDate = FUTURE.plusDays(2);

        a.edit("doc-1", newDate, AppointmentStatus.CANCELLED);

        assertThat(a.scheduledAt()).isEqualTo(newDate);
        assertThat(a.status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void edit_rejectsPastDate() {
        Appointment a = Appointment.schedule("pat-1", "doc-1", FUTURE);

        assertThatThrownBy(() -> a.edit("doc-1", LocalDateTime.now().minusDays(1), null))
                .isInstanceOf(InvalidAppointmentException.class);
    }

    @Test
    void edit_nullArgumentsLeaveFieldsUntouched() {
        Appointment a = Appointment.schedule("pat-1", "doc-1", FUTURE);

        a.edit("doc-1", null, null);

        assertThat(a.scheduledAt()).isEqualTo(FUTURE);
        assertThat(a.status()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void rehydrate_doesNotRevalidate() {
        LocalDateTime past = LocalDateTime.now().minusYears(1);

        Appointment a = Appointment.rehydrate("id-1", "pat-1", "doc-1", past,
                AppointmentStatus.COMPLETED, past);

        assertThat(a.id()).isEqualTo("id-1");
        assertThat(a.status()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(a.scheduledAt()).isEqualTo(past);
    }
}
