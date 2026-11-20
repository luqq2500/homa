package com.homa.app.outbox.sample.standing.usecase.port.out;

/** The sample consumer's read model of customers. Recording is deliberately not idempotent on its own. */
public interface CustomerStandings {

    void recordRegistration(String customerId, String city);
}
