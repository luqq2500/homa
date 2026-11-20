/**
 * The transactional outbox for cross-context integration events (docs/decisions/0002).
 *
 * <ul>
 *   <li>{@link com.homa.app.outbox.OutboxWriter} records each event a context publishes, in the publisher's
 *       transaction.</li>
 *   <li>{@link com.homa.app.outbox.OutboxRelay} delivers the rows to consuming contexts as
 *       {@link com.homa.ie.port.RelayedIE}, at least once and in order per aggregate.</li>
 *   <li>{@link com.homa.app.outbox.IntegrationEventJson} maps the payload; only {@code com.homa.ie.events} records
 *       are accepted.</li>
 * </ul>
 *
 * No context depends on this package: publishers and listeners only see Spring's event API and {@code com.homa.ie}.
 */
package com.homa.app.outbox;
