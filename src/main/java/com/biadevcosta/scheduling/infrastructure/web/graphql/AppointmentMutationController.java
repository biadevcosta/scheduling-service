package com.biadevcosta.scheduling.infrastructure.web.graphql;

import com.biadevcosta.scheduling.application.command.EditAppointmentCommand;
import com.biadevcosta.scheduling.application.command.ScheduleAppointmentCommand;
import com.biadevcosta.scheduling.application.usecase.EditAppointmentUseCase;
import com.biadevcosta.scheduling.application.usecase.ScheduleAppointmentUseCase;
import com.biadevcosta.scheduling.domain.Appointment;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * GraphQL adapter. The coarse role gate is here via {@code @PreAuthorize}; ownership and the
 * "patient only own" rule live in the domain / use cases.
 */
@Controller
public class AppointmentMutationController {

    private final ScheduleAppointmentUseCase scheduleUseCase;
    private final EditAppointmentUseCase editUseCase;

    public AppointmentMutationController(ScheduleAppointmentUseCase scheduleUseCase,
                                        EditAppointmentUseCase editUseCase) {
        this.scheduleUseCase = scheduleUseCase;
        this.editUseCase = editUseCase;
    }

    @QueryMapping
    public String _ping() {
        return "ok";
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE')")
    public AppointmentView scheduleAppointment(@Argument ScheduleInput input) {
        Appointment appointment = scheduleUseCase.execute(new ScheduleAppointmentCommand(
                input.patientId(), input.doctorId(), LocalDateTime.parse(input.scheduledAt())));
        return AppointmentView.from(appointment);
    }

    @MutationMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public AppointmentView editAppointment(@Argument EditInput input, @AuthenticationPrincipal Jwt jwt) {
        Appointment appointment = editUseCase.execute(new EditAppointmentCommand(
                input.appointmentId(),
                input.scheduledAt() == null ? null : LocalDateTime.parse(input.scheduledAt()),
                input.status(),
                jwt.getSubject()));
        return AppointmentView.from(appointment);
    }

    public record ScheduleInput(String patientId, String doctorId, String scheduledAt) {
    }

    public record EditInput(String appointmentId, String scheduledAt, String status) {
    }

    public record AppointmentView(String id, String patientId, String doctorId, String scheduledAt, String status) {
        static AppointmentView from(Appointment a) {
            return new AppointmentView(a.id(), a.patientId(), a.doctorId(),
                    a.scheduledAt().toString(), a.status().name());
        }
    }
}
