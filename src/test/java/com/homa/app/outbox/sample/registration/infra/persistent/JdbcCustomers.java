package com.homa.app.outbox.sample.registration.infra.persistent;

import com.homa.app.outbox.sample.registration.usecase.port.out.Customers;
import org.springframework.jdbc.core.simple.JdbcClient;

public class JdbcCustomers implements Customers {

    private final JdbcClient jdbc;

    public JdbcCustomers(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void add(String customerId, String city) {
        jdbc.sql("INSERT INTO sample_customer (customer_id, city) VALUES (:customerId, :city)")
                .param("customerId", customerId).param("city", city).update();
    }
}
