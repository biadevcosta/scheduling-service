package com.biadevcosta.scheduling.infrastructure.persistence;

import com.biadevcosta.scheduling.application.port.AppointmentRepository;
import com.biadevcosta.scheduling.domain.Appointment;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** {@link AppointmentRepository} backed by Spring Data JDBC. */
@Repository
public class AppointmentRepositoryImpl implements AppointmentRepository {

    private final AppointmentJdbcRepository jdbc;

    public AppointmentRepositoryImpl(AppointmentJdbcRepository jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Appointment save(Appointment appointment) {
        AppointmentEntity entity = jdbc.findById(appointment.id())
                .map(existing -> {
                    AppointmentMapper.copyInto(appointment, existing); // keep @Version -> UPDATE
                    return existing;
                })
                .orElseGet(() -> AppointmentMapper.toNewEntity(appointment)); // version null -> INSERT
        return AppointmentMapper.toDomain(jdbc.save(entity));
    }

    @Override
    public Optional<Appointment> findById(String id) {
        return jdbc.findById(id).map(AppointmentMapper::toDomain);
    }
}
