package com.homa.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.homa.ie.events.CustomerRegisteredIE;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/** The deployable as ECS/Fargate runs it: migrated, probed over HTTP, relaying the outbox on its own schedule. */
@SpringBootTest(classes = HomaApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "homa.outbox.relay.fixed-delay=PT0.1S")
@Import(PostgresContainer.class)
class HomaApplicationIT {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private TransactionTemplate transaction;

    @Test
    @DisplayName("The application starts against PostgreSQL with every migration applied.")
    void applicationStartsWithItsSchemaMigrated() {
        Integer applied = jdbc.sql("SELECT count(*) FROM flyway_schema_history WHERE success").query(Integer.class)
                .single();

        assertThat(applied).isPositive();
    }

    @Test
    @DisplayName("The container health check finds the process alive.")
    void livenessProbeReportsUp() {
        ResponseEntity<String> liveness = http.getForEntity("/actuator/health/liveness", String.class);

        assertThat(liveness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(liveness.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("The load balancer finds the task ready once its database is reachable.")
    void readinessProbeReportsUpWithTheDatabase() {
        ResponseEntity<String> readiness = http.getForEntity("/actuator/health/readiness", String.class);

        assertThat(readiness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readiness.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("Only health is exposed over HTTP.")
    void otherActuatorEndpointsAreNotExposed() {
        ResponseEntity<String> env = http.getForEntity("/actuator/env", String.class);

        assertThat(env.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("The running application relays a published event on its own schedule.")
    void scheduledRelayPublishesTheOutbox() {
        String eventId = UUID.randomUUID().toString();
        transaction.executeWithoutResult(status -> events.publishEvent(
                new CustomerRegisteredIE(eventId, "customer-aisyah", "SALAK_TINGGI", "ACTIVE", Instant.now())));

        await().atMost(Duration.ofSeconds(10)).until(() -> jdbc
                .sql("SELECT published_at IS NOT NULL FROM outbox_event WHERE event_id = :eventId")
                .param("eventId", eventId).query(Boolean.class).single());
    }
}
