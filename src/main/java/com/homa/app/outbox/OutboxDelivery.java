package com.homa.app.outbox;

import com.homa.ie.port.IntegrationEvent;
import com.homa.ie.port.RelayedIE;

/** The relay's envelope for one outbox row. */
record OutboxDelivery<E extends IntegrationEvent>(E event) implements RelayedIE<E> {
}
