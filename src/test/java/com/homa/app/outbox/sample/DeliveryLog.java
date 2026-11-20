package com.homa.app.outbox.sample;

import com.homa.ie.events.CustomerRegisteredIE;
import java.util.ArrayList;
import java.util.List;

/** Test only: what each sample listener received, before any de-duplication, so the mechanism itself is measured. */
public class DeliveryLog {

    public record Delivery(String listener, String eventId, String customerId, boolean inTransaction) {
    }

    private final List<Delivery> deliveries = new ArrayList<>();

    public synchronized void inPublisherTransaction(CustomerRegisteredIE event, boolean inTransaction) {
        deliveries.add(new Delivery("registration", event.eventId(), event.customerId(), inTransaction));
    }

    public synchronized void relayed(String listener, CustomerRegisteredIE event) {
        deliveries.add(new Delivery(listener, event.eventId(), event.customerId(), true));
    }

    public synchronized void relayedOtherType(String listener, String eventId) {
        deliveries.add(new Delivery(listener + ":other-type", eventId, null, true));
    }

    /** What one listener received, in the order it received it. */
    public synchronized List<Delivery> receivedBy(String listener) {
        return deliveries.stream().filter(d -> d.listener().equals(listener)).toList();
    }

    public synchronized List<String> eventIdsReceivedBy(String listener) {
        return receivedBy(listener).stream().map(Delivery::eventId).toList();
    }

    public synchronized void clear() {
        deliveries.clear();
    }

    public synchronized List<Delivery> all() {
        return new ArrayList<>(deliveries);
    }
}
