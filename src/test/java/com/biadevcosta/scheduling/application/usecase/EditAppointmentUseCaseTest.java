package com.biadevcosta.scheduling.application.usecase;

import com.biadevcosta.scheduling.application.command.EditAppointmentCommand;
import com.biadevcosta.scheduling.application.port.AppointmentEventPublisher;
import com.biadevcosta.scheduling.application.port.AppointmentRepository;
import com.biadevcosta.scheduling.domain.Appointment;
import com.biadevcosta.scheduling.domain.AppointmentStatus;
import com.biadevcosta.scheduling.domain.exception.AppointmentNotFoundException;
import com.biadevcosta.scheduling.domain.exception.InvalidAppointmentException;
import com.biadevcosta.scheduling.domain.exception.NotAppointmentOwnerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditAppointmentUseCaseTest {

    @Mock
    AppointmentRepository repository;
    @Mock
    AppointmentEventPublisher eventPublisher;

    EditAppointmentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EditAppointmentUseCase(repository, eventPublisher);
    }

    private Appointment existing(String doctorId) {
        return Appointment.rehydrate("apt-1", "pat-1", doctorId,
                LocalDateTime.now().plusDays(1), AppointmentStatus.SCHEDULED, LocalDateTime.now());
    }

    @Test
    void owner_editsThenPersistsThenPublishes() {
        when(repository.findById("apt-1")).thenReturn(Optional.of(existing("doc-1")));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var command = new EditAppointmentCommand("apt-1", null, "CANCELLED", "doc-1");

        Appointment result = useCase.execute(command);

        assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(repository).save(any());
        verify(eventPublisher).publishUpdated(any());
    }

    @Test
    void nonOwner_isRejected_andNothingIsSavedOrPublished() {
        when(repository.findById("apt-1")).thenReturn(Optional.of(existing("doc-1")));
        var command = new EditAppointmentCommand("apt-1", null, "CANCELLED", "doc-2");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(NotAppointmentOwnerException.class);

        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void unknownAppointment_throwsNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        var command = new EditAppointmentCommand("missing", null, null, "doc-1");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void unknownStatusValue_throwsInvalid() {
        when(repository.findById("apt-1")).thenReturn(Optional.of(existing("doc-1")));
        var command = new EditAppointmentCommand("apt-1", null, "NOPE", "doc-1");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidAppointmentException.class);
    }
}
