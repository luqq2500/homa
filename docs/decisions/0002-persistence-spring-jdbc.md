# 0002. Persist with Spring JDBC; deliver events through a transactional outbox

- Status: accepted
- Date: 2026-11-20
- Decided by: the product owner (Spring JDBC). The outbox and idempotency design follows the plan's "Delivery, idempotency and consistency" and pre-kickoff review items 2 and 3.

## Persistence

- **Spring JDBC.** Each context's `infra/persistent` uses `JdbcClient` with hand-written SQL and rebuilds its aggregates itself. No JPA and no jOOQ, so no lazy loading, dirty checking or generated code stands between an aggregate and its table.
- **PostgreSQL**, with the schema in Flyway migrations under `src/main/resources/db/migration`, one numbered file per change. Flyway runs at startup.
- **Transactions** come from Spring's `DataSourceTransactionManager`. `app` wraps each usecase in a transaction decorator, so usecase code carries no annotation. Everything a usecase writes through `JdbcClient`, the outbox row included, joins that one transaction.
- **Tests.** Persistence and outbox tests are `*IT` classes run by failsafe against Testcontainers PostgreSQL (`@ServiceConnection`).

## Cross-context delivery: the transactional outbox

```text
publisher's transaction                             relay transaction, one per row
┌───────────────────────────────────────────┐       ┌───────────────────────────────────────────────┐
│ save aggregate                             │       │ claim next due row FOR UPDATE SKIP LOCKED     │
│ publisher.publish(event)                   │       │ publish RelayedIE<Event> (synchronous)        │
│  └ OutboxWriter (MANDATORY tx) → INSERT    │ ────► │  └ each consumer's listener → its handler     │
│  └ own-context listeners (bare event)      │       │       (REQUIRES_NEW, ProcessedMessageStore)   │
└───────────────────────────────────────────┘       │ mark published                                │
                                                    └───────────────────────────────────────────────┘
```

1. **Publishing.** A context's single IE publisher in `infra/event` calls `ApplicationEventPublisher.publishEvent(event)` and nothing else. `com.homa.app.outbox.OutboxWriter` listens for every `IntegrationEvent`, synchronously, and inserts it into `outbox_event` in the caller's transaction. It requires one (`Propagation.MANDATORY`), so the row commits or rolls back with the aggregate change, and an event published outside a transaction fails. No context depends on `com.homa.app`: the publisher sees only Spring and `com.homa.ie`.
2. **Same-context handling.** A listener on the bare event type runs inside the publisher's transaction. Only the publishing context may do this, so a failure in another context can never roll back the publisher. ArchitectureTest rule 8 checks it: a listener in `com.homa.<context>.infra.event` may take only events of its own port, `<Context>IE`.
3. **Relaying.** `OutboxRelay`, scheduled in every task, handles one row per transaction. It claims the oldest due row whose aggregate has no earlier unpublished row (`FOR UPDATE SKIP LOCKED`), deserializes it, and publishes it synchronously as `RelayedIE<TheEventIE>`. It marks the row published in the same transaction, so a row is published only after every listener returned. Spring matches the envelope by its generic type (`PayloadApplicationEvent` with an explicit `ResolvableType`), so `RelayedIE<PaymentHeldIE>` reaches only PaymentHeldIE listeners.
4. **Consuming.** A consuming context's listener in `infra/event` listens for `RelayedIE<TheEventIE>` with a plain `@EventListener`, never `@TransactionalEventListener` or `@Async` (rule 8). It calls the handler interface in the handler's own transaction (`REQUIRES_NEW`).
5. **Failure.** If a listener throws, the relay transaction rolls back and the row stays unpublished. The relay records `attempts`, `last_error` and a backoff of 2, 4, 8 ... seconds, capped at 5 minutes. A consumer that had already succeeded keeps its committed effect; on the retry its ProcessedMessageStore turns the repeat into a no-op. A row that cannot be read (unknown type, bad payload) is held and reported the same way, never dropped.

### Guarantees

- **At least once.** A row leaves the outbox only when its published mark commits after every listener returned.
- **Ordered per aggregate.** The aggregate is the event's record component right after `eventId` (`reservationId`, `checkoutId`, `providerId`, ...), stored as `aggregate_type`/`aggregate_id`. A row is claimable only when no earlier row of its aggregate is unpublished, and a claimed row is locked. So even with many relays, the events of one aggregate are delivered one after another, in id order, and a failed event holds back the later ones of its aggregate only. Ids follow commit order within an aggregate because one transaction changes one aggregate under optimistic locking. Unit tests check that every catalog event follows the convention.
- **Never delivered twice by the mechanism.** The relay publishes only `RelayedIE` envelopes, and the publisher only bare events. A `RelayedIE` is not an `IntegrationEvent`, so the outbox writer never re-records a delivery, and no listener receives both kinds for one event. Two relays never hold the same row. A repeat happens only after a failure (a listener threw, or the relay stopped before its commit), and the consumer's ProcessedMessageStore absorbs it.
- **Safe payloads.** `IntegrationEventJson` (in `app`, never `kernel` or `ie`) writes the record with its own Jackson mapper. `Money` is written as `{"amount":"39.375","currency":"MYR"}` and `Percentage` as `"15.5"`, both exact strings. It reads back only a record named `*IE` in `com.homa.ie.events` that implements `IntegrationEvent`; no other class can be loaded from a row. Unknown fields are ignored, so an old row survives a removed field.

### Not yet done

Delivered rows and `processed_message` keys carry only ids, so they are technical rows, not business records: `TechnicalRowPurge` deletes those older than 30 days (`homa.technical-rows.retention`, default `P30D`) once a day at 03:17 Kuala Lumpur time (`homa.technical-rows.purge-cron`), as the development plan's "Data retention" section sets. An undelivered row is never purged. Business records follow the rules' §10 (DAT-1 to DAT-10), each owned and purged by the context that stores it.

## Idempotent handlers: ProcessedMessageStore

The shared table `processed_message (event_id, handler, processed_at)` has the primary key `(event_id, handler)`. Every consuming context adds the same three pieces, as the test-only sample in `src/test/java/com/homa/app/outbox/sample` shows:

1. **The port**, in `<context>/usecase/port/out/ProcessedMessageStore.java`:

   ```java
   public interface ProcessedMessageStore {
       /** True the first time this handler handles this event; false on a repeat, which must do nothing. */
       boolean recordFirstHandling(String eventId, String handler);
   }
   ```

2. **The JDBC implementation**, in `<context>/infra/persistent/JdbcProcessedMessageStore.java`:

   ```java
   return jdbc.sql("""
           INSERT INTO processed_message (event_id, handler) VALUES (:eventId, :handler)
           ON CONFLICT (event_id, handler) DO NOTHING
           """)
           .param("eventId", eventId).param("handler", handler)
           .update() == 1;
   ```

   A concurrent duplicate waits on the primary key, then inserts nothing and returns false.

3. **The handler**, in `<context>/usecase/ie/Handle<Event>IE.java`, records first and returns on a repeat:

   ```java
   static final String HANDLER = "reservation.PaymentHeld";   // stable: never derived from the class name

   public void handle(PaymentHeldIE event) {
       if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
           return;
       }
       // ... the effect
   }
   ```

   The record and the effect share the handler's transaction, which the listener starts. If the effect fails, the record rolls back with it and the retry runs the handler again. Money calls to the gateway also carry an idempotency key equal to the settlement line id (plan), because an external call cannot join the transaction.

## What a context adds in `infra/event`

- **Publisher:** `Spring<Context>IEPublisher implements <Context>IEPublisher` (the port in `usecase/port/out`), whose `publish(<Context>IE event)` is `events.publishEvent(event)`. It is called inside the usecase's transaction.
- **Listener:** `Spring<Context>IEListener` with one method per consumed event. Name each method for its event: `RelayedIE<A>` and `RelayedIE<B>` have the same erasure, so two methods both called `on` do not compile.

  ```java
  @EventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onPaymentHeld(RelayedIE<PaymentHeldIE> relayed) {
      paymentHeldHandler.handle(relayed.event());   // the <Event>IEHandler interface from usecase/port/in
  }
  ```

- **Persistence:** `JdbcProcessedMessageStore` in `infra/persistent`, as above, wired by `app` into each handler.
- **A new event** added to `com.homa.ie.events` must put its aggregate id (a String named `*Id`) right after `eventId`. Its publisher's port must extend `IntegrationEvent`. `OutboxPayloadTest` and `OutboxOrderingKeyTest` cover it automatically and fail on a component type the outbox JSON has not seen.
