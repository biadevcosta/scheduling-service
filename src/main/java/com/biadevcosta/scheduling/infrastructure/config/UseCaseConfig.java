package com.biadevcosta.scheduling.infrastructure.config;

import com.biadevcosta.scheduling.application.port.AppointmentEventPublisher;
import com.biadevcosta.scheduling.application.port.AppointmentRepository;
import com.biadevcosta.scheduling.application.port.ReminderPublisher;
import com.biadevcosta.scheduling.application.usecase.EditAppointmentUseCase;
import com.biadevcosta.scheduling.application.usecase.ScheduleAppointmentUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the plain-POJO use cases as beans. Use cases carry no Spring annotations themselves. */
@Configuration
public class UseCaseConfig {

    @Bean
    ScheduleAppointmentUseCase scheduleAppointmentUseCase(AppointmentRepository repository,
                                                          ReminderPublisher reminderPublisher,
                                                          AppointmentEventPublisher eventPublisher) {
        return new ScheduleAppointmentUseCase(repository, reminderPublisher, eventPublisher);
    }

    @Bean
    EditAppointmentUseCase editAppointmentUseCase(AppointmentRepository repository,
                                                 AppointmentEventPublisher eventPublisher) {
        return new EditAppointmentUseCase(repository, eventPublisher);
    }
}
