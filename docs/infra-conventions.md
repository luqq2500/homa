# Infra layer conventions

How every context writes its `infra` layer and its wiring in `app`, so six contexts built in parallel fit together.
It applies the backend module discipline, ADR 0001 (ECS/Fargate), ADR 0002 (Spring JDBC, outbox, idempotency) and
`docs/usecase-conventions.md`. Where this page and the plan disagree, the plan wins and this page is corrected.

## Scope for slice 1

Built now: `infra/persistent`, `infra/event`, `infra/transport/messaging` (schedulers and vendor webhooks),
`infra/transport/client` (vendor clients behind `port/out`), and the context's wiring in `app`.

Not yet: `infra/transport/controller` (REST) and `infra/security`. They wait for the choice of identity provider
(the plan: "buy: Cognito or similar"). Until then use cases are driven through their `port/in` interfaces, which is
how the end-to-end flow A test drives them.

## Layout

```text
com.homa.<context>.infra/
├── persistent/   Jdbc<Aggregate>Repository implements <Aggregate>Repository, Jdbc<Replica>..., JdbcProcessedMessageStore
├── event/        Spring<Context>IEPublisher implements <Context>IEPublisher; Spring<Context>IEListener
├── transport/
│   ├── messaging/  <Name>Schedule (@Scheduled, calls a port/in), <Vendor>Webhook adapters
│   └── client/     <Vendor><Port> implements a port/out client
└── time/         ClockTimeSource implements TimeSource; UuidEventIds implements EventIds

com.homa.app.wiring/
└── <Context>Wiring   @Configuration: builds each use case and handler from the context's adapters and exposes
                      it as a bean of its port/in type, wrapped by TransactionalPort.wrap(...) for use cases
```

## Rules

1. **Persistence is Spring JDBC** (`JdbcClient`, hand-written SQL). Rebuild aggregates only through the domain's
   `restore`/`rebuild` factories. Never set fields by reflection.
2. **One Flyway migration per context**, numbered so parallel work can't collide:
   `V2__customer.sql`, `V3__verification.sql`, `V4__provider.sql`, `V5__listing.sql`, `V6__reservation.sql`,
   `V7__payment.sql`. Table names are prefixed with the context (`customer_account`, `reservation_schedule_block`,
   ...). Never edit `V1`.
3. **Uniqueness rules are backed by unique indexes**: a sign-in per customer (CUS-7), a personId per customer
   (CUS-10), an identity number per person (IDN-3), and checkoutId, reservationId and providerId keys in Payment.
   Map a unique violation to the same domain refusal the domain would raise.
4. **Optimistic locking** uses a `version` column: `UPDATE ... SET version = :loaded + 1 WHERE id = :id AND version =
   :loaded`; 0 rows updated means a conflict, raised as the port's conflict exception. A new row inserts at
   version 0, and a duplicate insert is a conflict too.
5. **Sensitive data**: the identity number and bank account numbers are stored encrypted (`infra/persistent`, a key
   from configuration; tests use a fixed test key). The identity number is found through a keyed hash (blind index),
   never by decrypting a column. Neither ever goes to a log.
6. **Events** follow ADR 0002 exactly: the publisher calls `publishEvent`, and the listener has one method per
   consumed event on `RelayedIE<...>`, annotated `@EventListener` and `@Transactional(propagation = REQUIRES_NEW)`,
   calling the `port/in` handler interface. Handlers are not wrapped by `TransactionalPort`; the listener's
   transaction is theirs.
7. **Time and ids**: `ClockTimeSource` reads the `java.time.Clock` bean that `app` provides (Asia/Kuala_Lumpur);
   `UuidEventIds` returns random UUIDs. Tests replace the `Clock` bean with a fixed one.
8. **Schedulers** run in every task (ADR 0001), so a scheduled use case must be safe to run concurrently. Claim work
   with row locks (`FOR UPDATE SKIP LOCKED`) or make it idempotent, and run each claimed item in its own transaction.
9. **Vendor clients** (payment provider, verification service) translate the vendor's answer into the port's fact
   and decide nothing (EXT-1). In tests they are WireMock-backed or a test double bean; no real vendor is called.
10. **The `Country policy`** instance (`CountryPolicy.MALAYSIA`) is chosen in `VerificationWiring`.

## Tests

- Each repository and store has an `*IT` against Testcontainers PostgreSQL (`@ServiceConnection`, see
  `src/test/java/com/homa/app/PostgresContainer.java`): save, restore through the domain factory, the optimistic
  conflict, and the unique indexes.
- Each listener has an `*IT` that relays a real outbox row and checks the handler's effect happens once.
- Infra has no mutation gate, but CRAP below 6 applies to every method.
- `mvn -B verify` (unit, ArchitectureTest, integration tests, PIT) and `python3 tools/quality_gates.py` must pass.
