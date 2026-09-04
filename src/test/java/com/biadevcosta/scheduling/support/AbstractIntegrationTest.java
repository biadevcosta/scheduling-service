package com.biadevcosta.scheduling.support;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared singleton containers (MySQL, RabbitMQ, Kafka). Started once for the whole test run and
 * left to Ryuk to reap; properties are bound with {@link DynamicPropertySource} so this works
 * regardless of which {@code @ServiceConnection} factories a Spring Boot version ships.
 *
 * <p>Requires a Docker daemon the docker-java client can reach. When none is available the whole
 * integration test is skipped (assumption failure) rather than failing the build.
 */
public abstract class AbstractIntegrationTest {

    private static final boolean DOCKER_AVAILABLE = isDockerAvailable();

    protected static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));

    protected static final RabbitMQContainer RABBIT =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

    protected static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    static {
        if (DOCKER_AVAILABLE) {
            MYSQL.start();
            RABBIT.start();
            KAFKA.start();
        }
    }

    @BeforeAll
    static void requireDocker() {
        assumeTrue(DOCKER_AVAILABLE, "Docker is not available - skipping integration test");
    }

    private static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);

        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
}
