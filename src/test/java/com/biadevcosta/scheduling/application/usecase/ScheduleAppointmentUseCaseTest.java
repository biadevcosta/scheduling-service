package com.biadevcosta.scheduling.application.usecase;

import com.biadevcosta.scheduling.application.command.ScheduleAppointmentCommand;
import com.biadevcosta.scheduling.application.port.AppointmentEventPublisher;
import com.biadevcosta.scheduling.application.port.AppointmentRepository;
import com.biadevcosta.scheduling.application.port.ReminderPublisher;
import com.biadevcosta.scheduling.domain.Appointment;
import com.biadevcosta.scheduling.domain.AppointmentStatus;
import com.biadevcosta.scheduling.domain.exception.InvalidAppointmentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleAppointmentUseCaseTest {

    @Mock
    AppointmentRepository repository;
    @Mock
    ReminderPublisher reminderPublisher;
    @Mock
    AppointmentEventPublisher eventPublisher;

    ScheduleAppointmentUseCase useCase;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        useCase = new ScheduleAppointmentUseCase(repository, reminderPublisher, eventPublisher);
    }

    @Test
    void persistsBeforePublishing_andReturnsScheduled() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var command = new ScheduleAppointmentCommand("pat-1", "doc-1", LocalDateTime.now().plusDays(1));

        Appointment result = useCase.execute(command);

        assertThat(result.patientId()).isEqualTo("pat-1");
        assertThat(result.status()).isEqualTo(AppointmentStatus.SCHEDULED);

        InOrder order = inOrder(repository, reminderPublisher, eventPublisher);
        order.verify(repository).save(any());
        order.verify(reminderPublisher).publishReminder(any());
        order.verify(eventPublisher).publishCreated(any());
    }

    @Test
    void invalidCommand_neverTouchesRepositoryOrBrokers() {
        var command = new ScheduleAppointmentCommand("pat-1", "doc-1", LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidAppointmentException.class);

        verifyNoInteractions(repository, reminderPublisher, eventPublisher);
    }
}
