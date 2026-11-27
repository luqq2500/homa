package com.homa.customer.usecase;

import com.homa.customer.usecase.port.out.CustomerIEPublisher;
import com.homa.ie.port.CustomerIE;
import java.util.ArrayList;
import java.util.List;

/** Keeps every event published, in order. */
final class RecordingCustomerIEPublisher implements CustomerIEPublisher {

    private final List<CustomerIE> published = new ArrayList<>();

    @Override
    public void publish(CustomerIE event) {
        published.add(event);
    }

    List<CustomerIE> published() {
        return List.copyOf(published);
    }
}
