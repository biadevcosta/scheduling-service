package com.biadevcosta.scheduling.infrastructure.persistence;

import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository over {@link AppointmentEntity}. The entity carries a {@code @Version}
 * field, so {@code save()} inserts when it is null and updates otherwise (the id is assigned by the
 * domain, not the database).
 */
public interface AppointmentJdbcRepository extends CrudRepository<AppointmentEntity, String> {
}
