package com.homa.app.outbox.sample.conversation.infra.persistent;

import com.homa.app.outbox.sample.FailureSwitch;
import com.homa.app.outbox.sample.conversation.usecase.port.out.Members;
import org.springframework.jdbc.core.simple.JdbcClient;

public class JdbcMembers implements Members {

    private final JdbcClient jdbc;
    private final FailureSwitch failures;

    public JdbcMembers(JdbcClient jdbc, FailureSwitch failures) {
        this.jdbc = jdbc;
        this.failures = failures;
    }

    @Override
    public void admit(String customerId) {
        jdbc.sql("""
                INSERT INTO sample_conversation_member (customer_id, times_recorded) VALUES (:customerId, 1)
                ON CONFLICT (customer_id) DO UPDATE SET times_recorded = sample_conversation_member.times_recorded + 1
                """)
                .param("customerId", customerId).update();
        failures.failIfSet("conversation", customerId);
    }
}
