package com.biadevcosta.scheduling.infrastructure.web.graphql;

import com.biadevcosta.scheduling.application.command.EditAppointmentCommand;
import com.biadevcosta.scheduling.application.command.ScheduleAppointmentCommand;
import com.biadevcosta.scheduling.application.usecase.EditAppointmentUseCase;
import com.biadevcosta.scheduling.application.usecase.ScheduleAppointmentUseCase;
import com.biadevcosta.scheduling.domain.Appointment;
import com.biadevcosta.scheduling.domain.AppointmentStatus;
import com.biadevcosta.scheduling.infrastructure.web.graphql.AppointmentMutationController.EditInput;
import com.biadevcosta.scheduling.infrastructure.web.graphql.AppointmentMutationController.ScheduleInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentMutationControllerTest {

    @Mock
    ScheduleAppointmentUseCase scheduleUseCase;
    @Mock
    EditAppointmentUseCase editUseCase;

    private static Appointment result(String status) {
        return Appointment.rehydrate("apt-1", "pat-1", "doc-1",
                LocalDateTime.of(2030, 9, 1, 14, 30), AppointmentStatus.valueOf(status),
                LocalDateTime.of(2030, 8, 1, 8, 0));
    }

    private static Jwt jwtFor(String subject) {
        return Jwt.withTokenValue("t").header("alg", "RS256")
                .subject(subject).claim("role", "DOCTOR")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    @Test
    void scheduleAppointment_delegatesAndMapsView() {
        when(scheduleUseCase.execute(any())).thenReturn(result("SCHEDULED"));
        var controller = new AppointmentMutationController(scheduleUseCase, editUseCase);

        var view = controller.scheduleAppointment(new ScheduleInput("pat-1", "doc-1", "2030-09-01T14:30:00"));

        ArgumentCaptor<ScheduleAppointmentCommand> captor =
                ArgumentCaptor.forClass(ScheduleAppointmentCommand.class);
        org.mockito.Mockito.verify(scheduleUseCase).execute(captor.capture());
        assertThat(captor.getValue().patientId()).isEqualTo("pat-1");
        assertThat(captor.getValue().scheduledAt()).isEqualTo(LocalDateTime.of(2030, 9, 1, 14, 30));
        assertThat(view.id()).isEqualTo("apt-1");
        assertThat(view.status()).isEqualTo("SCHEDULED");
        assertThat(view.scheduledAt()).isEqualTo("2030-09-01T14:30");
    }

    @Test
    void editAppointment_passesCallerFromJwtSubject() {
        when(editUseCase.execute(any())).thenReturn(result("CANCELLED"));
        var controller = new AppointmentMutationController(scheduleUseCase, editUseCase);

        var view = controller.editAppointment(new EditInput("apt-1", null, "CANCELLED"), jwtFor("doc-1"));

        ArgumentCaptor<EditAppointmentCommand> captor =
                ArgumentCaptor.forClass(EditAppointmentCommand.class);
        org.mockito.Mockito.verify(editUseCase).execute(captor.capture());
        assertThat(captor.getValue().callerId()).isEqualTo("doc-1");
        assertThat(captor.getValue().newScheduledAt()).isNull();
        assertThat(captor.getValue().newStatus()).isEqualTo("CANCELLED");
        assertThat(view.status()).isEqualTo("CANCELLED");
    }

    @Test
    void editAppointment_parsesScheduledAtWhenPresent() {
        when(editUseCase.execute(any())).thenReturn(result("SCHEDULED"));
        var controller = new AppointmentMutationController(scheduleUseCase, editUseCase);

        controller.editAppointment(new EditInput("apt-1", "2031-01-02T09:15:00", null), jwtFor("doc-1"));

        ArgumentCaptor<EditAppointmentCommand> captor =
                ArgumentCaptor.forClass(EditAppointmentCommand.class);
        org.mockito.Mockito.verify(editUseCase).execute(captor.capture());
        assertThat(captor.getValue().newScheduledAt()).isEqualTo(LocalDateTime.of(2031, 1, 2, 9, 15));
    }

    @Test
    void ping_returnsOk() {
        var controller = new AppointmentMutationController(scheduleUseCase, editUseCase);
        assertThat(controller._ping()).isEqualTo("ok");
    }
}
