package com.biadevcosta.scheduling.infrastructure.persistence;

import com.biadevcosta.scheduling.domain.Appointment;
import com.biadevcosta.scheduling.domain.AppointmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentPersistenceTest {

    @Mock
    AppointmentJdbcRepository jdbc;

    private static Appointment sample(String id) {
        return Appointment.rehydrate(id, "pat-1", "doc-1",
                LocalDateTime.of(2030, 1, 1, 10, 0), AppointmentStatus.SCHEDULED,
                LocalDateTime.of(2029, 12, 1, 9, 0));
    }

    @Test
    void mapper_roundTripsThroughEntity() {
        Appointment original = sample("apt-1");

        AppointmentEntity entity = AppointmentMapper.toNewEntity(original);
        Appointment back = AppointmentMapper.toDomain(entity);

        assertThat(entity.getStatus()).isEqualTo("SCHEDULED");
        assertThat(back.id()).isEqualTo("apt-1");
        assertThat(back.patientId()).isEqualTo("pat-1");
        assertThat(back.doctorId()).isEqualTo("doc-1");
        assertThat(back.scheduledAt()).isEqualTo(original.scheduledAt());
        assertThat(back.status()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void save_newAppointment_insertsWithNullVersion() {
        when(jdbc.findById("apt-1")).thenReturn(Optional.empty());
        when(jdbc.save(any())).thenAnswer(i -> i.getArgument(0));
        var repository = new AppointmentRepositoryImpl(jdbc);

        repository.save(sample("apt-1"));

        ArgumentCaptor<AppointmentEntity> captor = ArgumentCaptor.forClass(AppointmentEntity.class);
        org.mockito.Mockito.verify(jdbc).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isNull();
        assertThat(captor.getValue().getId()).isEqualTo("apt-1");
    }

    @Test
    void save_existingAppointment_keepsVersionForUpdate() {
        AppointmentEntity existing = AppointmentMapper.toNewEntity(sample("apt-1"));
        existing.setVersion(7L);
        when(jdbc.findById("apt-1")).thenReturn(Optional.of(existing));
        when(jdbc.save(any())).thenAnswer(i -> i.getArgument(0));
        var repository = new AppointmentRepositoryImpl(jdbc);

        Appointment edited = sample("apt-1");
        edited.edit("doc-1", null, AppointmentStatus.CANCELLED);
        repository.save(edited);

        ArgumentCaptor<AppointmentEntity> captor = ArgumentCaptor.forClass(AppointmentEntity.class);
        org.mockito.Mockito.verify(jdbc).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(7L);
        assertThat(captor.getValue().getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void findById_mapsEntityToDomain() {
        when(jdbc.findById("apt-1")).thenReturn(Optional.of(AppointmentMapper.toNewEntity(sample("apt-1"))));
        var repository = new AppointmentRepositoryImpl(jdbc);

        Optional<Appointment> found = repository.findById("apt-1");

        assertThat(found).isPresent();
        assertThat(found.get().patientId()).isEqualTo("pat-1");
    }
}
