# scheduling-service

Write side of the hospital appointment system. Owns the `appointments` table (source of truth),
exposes a GraphQL API guarded by a stateless RS256 JWT, and on every create/edit **persists first,
then publishes**:

- **RabbitMQ** `reminder.queue` — `AppointmentReminderMessage` (ids only) for `notification-service`.
- **Kafka** `appointment-events` — `AppointmentCreated` / `AppointmentUpdated`, key = `patientId`,
  for `history-service`.

Port: **8081**.

## Architecture (Clean Architecture)

```
domain/          Appointment aggregate + business rules, no framework imports
  Appointment, AppointmentStatus, exception/*
application/     use cases as POJOs + ports (interfaces) + commands
  usecase/ScheduleAppointmentUseCase, EditAppointmentUseCase
  port/AppointmentRepository, ReminderPublisher, AppointmentEventPublisher
  command/ScheduleAppointmentCommand, EditAppointmentCommand
infrastructure/  everything Spring
  persistence/   Spring Data JDBC entity + mapper + AppointmentRepositoryImpl
  messaging/     rabbit/* and kafka/* adapters + message records
  security/      SecurityConfig (JWT resource server, role -> ROLE_ authority)
  web/graphql/   AppointmentMutationController + DomainExceptionResolver
  config/        UseCaseConfig wires the POJO use cases with @Bean
```

Rule enforcement:

| Rule | Where |
|---|---|
| Role gate (`DOCTOR`/`NURSE` schedule, `DOCTOR` edit) | `@PreAuthorize` on the resolver |
| Only the owner doctor edits | `Appointment.edit(callerDoctorId, ...)` in the domain |
| `scheduledAt` must be in the future | `Appointment.schedule` / `Appointment.edit` |
| Persist before publish | `ScheduleAppointmentUseCase` / `EditAppointmentUseCase` |

## Configuration (`src/main/resources/application.yaml`)

| Key | Default | Notes |
|---|---|---|
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/scheduling_db` | Flyway migrates `V1__create_appointments.sql` |
| `spring.rabbitmq.*` | `localhost:5672` guest/guest | |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | JSON serializer |
| `security.jwt.public-key` | `classpath:public.pem` | RS256 public key, shared with `identity-service` |
| `app.rabbit.exchange` / `queue` / `routing-key` | `appointment.reminders` / `reminder.queue` / `appointment.reminder` | |
| `app.kafka.topic` | `appointment-events` | |

## Run locally

```bash
# 1. backing services (MySQL + RabbitMQ + Kafka)
docker compose up -d

# 2. the service
./mvnw spring-boot:run
```

GraphiQL: <http://localhost:8081/graphiql>. Add an `Authorization: Bearer <token>` header
(get a token from `identity-service` `POST /auth/login`), then:

```graphql
mutation {
  scheduleAppointment(input: {
    patientId: "pat-1", doctorId: "doc-1", scheduledAt: "2030-12-01T10:00:00"
  }) { id status }
}
```

```graphql
mutation {
  editAppointment(input: { appointmentId: "<id>", status: "CANCELLED" }) { id status }
}
```

Check the message in the RabbitMQ UI (<http://localhost:15672>, guest/guest) and the row in
`appointments`.

## Tests

```bash
./mvnw test        # unit tests (domain + use cases), no Docker needed
./mvnw verify      # + SchedulingIntegrationTest (Testcontainers: MySQL/RabbitMQ/Kafka) + JaCoCo 80% gate
./mvnw allure:serve # test report
```

`SchedulingIntegrationTest` needs a running Docker daemon. It signs its own RS256 tokens with an
in-memory keypair (`support/SecurityTestConfig`) and drives the real GraphQL + security stack.

Coverage report: `target/site/jacoco/index.html`. `*Application`, `infrastructure/config/**` and
message records are excluded from the 80% line gate.

## Build image

```bash
docker build -t scheduling-service .
```
