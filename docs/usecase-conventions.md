# Usecase layer conventions

How every context writes its `usecase` layer, so that six contexts built in parallel read the same. It applies the
backend module discipline (`docs/backend_module_discipline.md`), the plan's "Use cases per context", and ADR 0002 for
events and idempotency. Where this page and the plan disagree, the plan wins and this page is corrected.

## Layout

```text
com.homa.<context>.usecase/
├── port/in/      one interface per use case, named for the actor (CustomerRegistrator), and one
│                 <Event>IEHandler interface per consumed event (PaymentHeldIEHandler)
├── port/out/     <Aggregate>Repository, <Context>IEPublisher, ProcessedMessageStore, TimeSource, clients
├── dto/          one request record and one response record per use case, never shared
├── exception/    orchestration failures only: <Thing>NotFoundException, NotAllowedException, ...
├── ie/           Handle<Event>IE implements <Event>IEHandler, one per event
└── <Actor><Action>.java   the use case implementations, named for the action (CustomerRegisterAccount)
```

## Rules

1. **One use case = one interface + one implementation**, in separate files. The interface has one method whose
   name says the intent (`register`, `accept`). It takes its request DTO and returns its response DTO.
2. **DTOs are records of Strings, numbers, `Instant`, `LocalDate`, `Money` and enum names as Strings.** Domain
   objects never cross the port, in either direction.
3. **A use case loads through `port/out`, calls domain behaviour, saves, and publishes at most the events the plan
   names.** It never computes a fee, a deadline, an age or a date. It asks the domain. If a rule seems to need an
   `if` here, the rule belongs in the domain: add it there, with its tests.
4. **Application policy lives here**: who may act (the actor's id is a field of the request; `infra/security` fills
   it from the token later), what happens next, which event to publish. Such checks throw a usecase exception.
5. **Time and ids come from ports.** `TimeSource { Instant now(); }` in `port/out`; never `Instant.now()`.
   Event ids come from a `port/out` `EventIds { String next(); }`, so tests can assert them. Domain ids keep
   their own `generate()`.
6. **Events.** Build each event from the saved aggregate, field by field, as the domain agents' reports and the
   plan's catalog map them. Publish through the context's single `<Context>IEPublisher.publish(<Context>IE)`.
7. **Handlers are idempotent** (ADR 0002): `Handle<Event>IE` first calls
   `processedMessages.recordFirstHandling(event.eventId(), HANDLER)` and returns on `false`. `HANDLER` is a stable
   string `"<context>.<Event>"`, never derived from the class name. A handler never lets a domain refusal escape
   in a way that would make the relay retry forever: it records the outcome (or publishes the refusal event the
   plan names) and returns.
8. **Uniqueness rules that span aggregates** (CUS-7 sign-in, CUS-10 personId, IDN-3 identity number, HLP-10
   affiliation): the use case looks the key up through its repository and passes the domain's status value
   (`SignInStatus`, `IdentityStatus`, `IdentityNumberStatus`, ...) into the aggregate, which refuses. A unique index
   in infra backs it up.
9. **No framework.** No Spring, Jakarta or cloud SDK imports; no annotations. Transactions are applied by `app`
   around each use case and each handler (ArchitectureTest rule 4 is an allowlist).

## Tests

- Plain JUnit 5 + AssertJ, with **in-memory fakes of the `port/out` interfaces** in the context's own test package
  (`com.homa.<context>.usecase`), e.g. `InMemoryCustomerRepository`, `RecordingCustomerIEPublisher`,
  `FixedTimeSource`, `SequentialEventIds`, `InMemoryProcessedMessageStore`. Fakes never contain rules.
- Test classes are named for the use case or handler (`CustomerRegisterAccountTest`,
  `HandlePaymentHeldIETest`). Methods are camelCase sentences; `@Tag("<rule ID>")` and `@DisplayName("<case>")`.
- Every handler has a test that a redelivered event changes nothing and publishes nothing.
- Every published event is asserted field by field.
- Gates: mutation score and test strength at least 80% for `<context>.usecase`; CRAP below 6 everywhere; the
  domain stays at its 100% ratchet.
