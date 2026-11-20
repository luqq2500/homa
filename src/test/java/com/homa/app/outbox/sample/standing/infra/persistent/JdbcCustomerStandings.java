package com.homa.app.outbox.sample.standing.infra.persistent;

import com.homa.app.outbox.sample.FailureSwitch;
import com.homa.app.outbox.sample.standing.usecase.port.out.CustomerStandings;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Counts every recording, so a duplicate effect would show as times_recorded 2. */
public class JdbcCustomerStandings implements CustomerStandings {

    private final JdbcClient jdbc;
    private final FailureSwitch failures;

    public JdbcCustomerStandings(JdbcClient jdbc, FailureSwitch failures) {
        this.jdbc = jdbc;
        this.failures = failures;
    }

    @Override
    public void recordRegistration(String customerId, String city) {
        jdbc.sql("""
                INSERT INTO sample_customer_standing (customer_id, city, times_recorded) VALUES (:customerId, :city, 1)
                ON CONFLICT (customer_id) DO UPDATE SET times_recorded = sample_customer_standing.times_recorded + 1
                """)
                .param("customerId", customerId).param("city", city).update();
        failures.failIfSet("standing", customerId);
    }
}
