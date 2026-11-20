package com.homa.ie.port;

/**
 * An integration event as it reaches a consuming context: relayed from the outbox after the publisher's
 * transaction committed. A context's listener in {@code infra/event} listens for {@code RelayedIE<TheEventIE>}
 * and passes {@link #event()} to its handler interface.
 *
 * <p>The bare event, published inside the publisher's own transaction, is never a RelayedIE. So a listener on the
 * bare type (allowed only inside the publishing context) and a listener on RelayedIE can never both receive the
 * same delivery.
 *
 * @param <E> the event being delivered
 */
public interface RelayedIE<E extends IntegrationEvent> {

    E event();
}
