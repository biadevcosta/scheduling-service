package com.biadevcosta.scheduling;

import com.biadevcosta.scheduling.support.AbstractIntegrationTest;
import com.biadevcosta.scheduling.support.SecurityTestConfig;
import com.biadevcosta.scheduling.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end: real MySQL/RabbitMQ/Kafka containers, real security filter chain, GraphQL over HTTP.
 * Requires a running Docker daemon.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@Import(SecurityTestConfig.class)
class SchedulingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    HttpGraphQlTester graphQlTester;

    @Autowired
    TestTokens tokens;

    private GraphQlTester asDoctor(String doctorId) {
        return graphQlTester.mutate()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.forUser(doctorId, "DOCTOR"))
                .build();
    }

    @Test
    void doctorSchedulesAppointment_returnsScheduled() {
        String id = asDoctor("doc-1").document("""
                        mutation {
                          scheduleAppointment(input: {
                            patientId: "pat-1", doctorId: "doc-1", scheduledAt: "2030-12-01T10:00:00"
                          }) { id status patientId }
                        }
                        """)
                .execute()
                .path("scheduleAppointment.status").entity(String.class).isEqualTo("SCHEDULED")
                .path("scheduleAppointment.patientId").entity(String.class).isEqualTo("pat-1")
                .path("scheduleAppointment.id").entity(String.class).get();

        assertThat(id).isNotBlank();
    }

    @Test
    void ownerDoctorEditsAppointment_thenStatusChanges() {
        GraphQlTester doctor = asDoctor("doc-1");
        String id = doctor.document("""
                        mutation {
                          scheduleAppointment(input: {
                            patientId: "pat-2", doctorId: "doc-1", scheduledAt: "2030-12-02T09:00:00"
                          }) { id }
                        }
                        """)
                .execute()
                .path("scheduleAppointment.id").entity(String.class).get();

        doctor.document("""
                        mutation Edit($id: ID!) {
                          editAppointment(input: { appointmentId: $id, status: "CANCELLED" }) { status }
                        }
                        """)
                .variable("id", id)
                .execute()
                .path("editAppointment.status").entity(String.class).isEqualTo("CANCELLED");
    }

    @Test
    void nonOwnerDoctorEdit_isForbiddenByDomain() {
        String id = asDoctor("doc-1").document("""
                        mutation {
                          scheduleAppointment(input: {
                            patientId: "pat-3", doctorId: "doc-1", scheduledAt: "2030-12-03T09:00:00"
                          }) { id }
                        }
                        """)
                .execute()
                .path("scheduleAppointment.id").entity(String.class).get();

        asDoctor("doc-2").document("""
                        mutation Edit($id: ID!) {
                          editAppointment(input: { appointmentId: $id, status: "CANCELLED" }) { status }
                        }
                        """)
                .variable("id", id)
                .execute()
                .errors()
                .expect(error -> "FORBIDDEN".equals(String.valueOf(error.getExtensions().get("classification"))));
    }

    @Test
    void nurseCannotEdit_roleGateRejects() {
        graphQlTester.mutate()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.forUser("nurse-1", "NURSE"))
                .build()
                .document("""
                        mutation {
                          editAppointment(input: { appointmentId: "whatever", status: "CANCELLED" }) { status }
                        }
                        """)
                .execute()
                .errors()
                .expect(error -> error.getMessage() != null);
    }

    @Test
    void missingToken_isUnauthorized() {
        graphQlTester.document("""
                        mutation {
                          scheduleAppointment(input: {
                            patientId: "pat-x", doctorId: "doc-x", scheduledAt: "2030-12-04T09:00:00"
                          }) { id }
                        }
                        """)
                .execute()
                .errors()
                .expect(error -> error.getMessage() != null);
    }
}
