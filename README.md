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

Everything in containers (builds the image, waits for MySQL / RabbitMQ / Kafka to be healthy):

```bash
docker compose up --build
```

That starts `mysql-scheduling` (3306), `rabbitmq` (5672 / management UI 15672, guest/guest),
`kafka` (9092), **`scheduling-service`** (host port **8085** → container 8081), plus two web
consoles for inspecting what the service emits:

| Tool | URL | Shows |
|---|---|---|
| RabbitMQ management | <http://localhost:15672> (guest/guest) | `reminder.queue` — *Queues → reminder.queue → Get messages* (Requeue = Yes to peek) |
| Kafka UI | <http://localhost:8084> | `appointment-events` — *Topics → appointment-events → Messages* (key / value / headers) |
| phpMyAdmin | <http://localhost:8083> (root/root) | `scheduling_db.appointments` |

> The container publishes on host **8085** because 8081 is often taken locally; change the
> `ports` mapping in `docker-compose.yml` if you want 8081. Running on the host (below) still
> uses 8081.

Or backing services only, app on the host (dev, port 8081):

```bash
docker compose up -d mysql-scheduling rabbitmq kafka
./mvnw spring-boot:run
```

Kafka advertises two listeners — `localhost:9092` for host clients and `kafka:19092` inside the
compose network — so both ways work.

## API docs (GraphQL)

This is a GraphQL API, so the interactive docs are **GraphiQL** (the GraphQL analog of Swagger UI),
not OpenAPI:

URLs below use host port **8085** (Docker). On the host (`./mvnw spring-boot:run`) it's **8081**.

| URL | What |
|---|---|
| <http://localhost:8085/graphiql> | GraphiQL explorer — schema browser + query runner. Open the **Headers** pane and add `{"Authorization":"Bearer <token>"}` (token from `identity-service` `POST /auth/login`), then run a mutation. |
| <http://localhost:8085/graphql/schema> | the SDL schema as text |
| `POST http://localhost:8085/graphql` | the endpoint itself (JWT required) |

`/graphiql`, `/graphiql/**` and `/graphql/schema` are `permitAll` in `SecurityConfig`; `/graphql`
requires a valid `DOCTOR`/`NURSE` token.

Example — add the `Authorization: Bearer <token>` header in GraphiQL, then:

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

### Insomnia collection

`scheduling.insomnia.json` covers `_ping`, `scheduleAppointment` (DOCTOR + NURSE), `editAppointment`
(owner + not-owner), the SDL/health endpoints and the error cases (401 / forbidden / past-date /
not-found). Import it, run **`0 · tokens → login (doctor)`** once (it hits `identity-service` on
`identity_url`) and the Bearer token chains into every other request. Set `caller_doctor_id` in the
environment for the owner-edit flow.

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
