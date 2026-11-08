# Backend Module Architecture & Engineering Discipline

This document establishes the engineering discipline, architectural boundaries, and co-location standards for backend module development. The principles are **layer-based and framework-agnostic**: they apply regardless of persistence technology, transport protocol, or messaging broker. The reference stack is **Java** with **Spring** as the infrastructure framework.

---

## 1. Core Architectural Pillars

Our backend architecture is built on three unyielding pillars:

- **Domain-Centric Design:** Every module follows Clean Architecture, SOLID, and Domain-Driven Design (DDD). Business logic is the center of the system; frameworks and delivery mechanisms are details at the edge.
- **Strict Encapsulation:** Each bounded context is a black box. It exposes behavior only through defined gateways (usecase ports inward, integration events outward).
- **Inward-Only Dependency Flow:** Dependencies point strictly inward, from infrastructure toward the domain. The domain depends on nothing except the shared kernel.

---

## 2. Module Taxonomy & Boundaries

The codebase is partitioned into **shared modules** and **bounded-context modules**.

### Shared Modules

- `kernel/`: The shared kernel. It holds *only* small, immutable, domain-agnostic value primitives that every bounded context's domain needs, such as `Money` and `Percentage`. It has no dependencies of its own (no framework, no other module), no entities, no repositories and no business rules of any one bounded context. It is the **only** module a `domain` layer may import.
- `ie/`: The integration event contract module. It holds *only* event interfaces and event definitions that other bounded contexts may depend on. It carries no behavior and no business logic. Events carry primitives, Strings, kernel types and JDK value types (`Instant`, `Duration`, immutable `List`/`Set`); never domain objects; no `Optional` fields. Collections are copied defensively in the record's compact constructor. `ie/` depends only on `kernel/`, and no `domain` layer imports it.
- Other shared modules hold domain-agnostic, cross-cutting primitives that more than one bounded context genuinely needs. Domain layers may not import them.

#### Shared Kernel Admission Rule

A type joins `kernel/` only when all of these hold:

- At least two bounded contexts' domains need it.
- It is a value object: immutable, compared by value, with no identity.
- It carries no rule specific to one bounded context (for example `Money` may know rounding; it must not know service fees or cancellation tiers).
- It depends on nothing outside the Java standard library.

Adding a type to `kernel/` is a deliberate change, reviewed like a contract change, because every bounded context depends on it.

### Bounded-Context Modules

Each bounded context is a self-contained module with exactly three layers:

- `domain`: The bounded context's business logic objects.
- `usecase`: The orchestration of domain objects to fulfil specific business intents.
- `infra`: The technical implementation of persistence, transport, security, and events.

---

## 3. The Co-location Discipline

Files that change together live together. Everything a layer needs (exceptions, DTOs, ports) resides within that layer's boundary.

### Bounded-Context Blueprint

```text
<bounded-context>/
├── domain/
│   ├── <Entity>.java                 # Entities, value objects, aggregates
│   ├── <Entity>Id.java               # Type-safe id generator
│   └── exception/                    # All domain-level exceptions
├── usecase/
│   ├── port/
│   │   ├── in/                       # Usecase interfaces, IE handler interfaces
│   │   └── out/                      # Outbound interfaces (repository, IE publisher)
│   ├── dto/                          # Dedicated request/response DTOs per usecase
│   ├── exception/                    # Usecase-level exceptions
│   └── <UsecaseImpl>.java            # Usecase and IE handler implementations
└── infra/
    ├── persistent/                   # Repository, database, caching
    ├── transport/
    │   ├── controller/               # REST controllers
    │   ├── messaging/                # Messaging infrastructure
    │   └── client/                   # Outbound clients (cloud SDKs, third-party APIs)
    ├── security/                     # Security infrastructure
    └── event/                        # Integration event publisher & listener
```

### Domain Layer

- Dedicated exclusively to the bounded context's business logic objects.
- Contains an `/exception` module that stores **all** domain-level exceptions.
- Is kept agnostic from every outside module: `usecase`, `infra`, any other shared module, and any framework. The only exception is the shared kernel, `kernel/`.
- Each domain object enforces its own business rules.
- Object naming follows the bounded context's business (ubiquitous) language.

### Usecase Layer

- Exists once the only thing missing in the domain is a **business logic orchestrator** that coordinates domain objects to perform a bounded-context-specific usecase.
- Directory layout: `/port` (with `/in` and `/out`), `/exception`, `/dto`.
- Owns its own usecase-level exceptions in `/exception`.
- Depends only on domain and shared modules.

### Infra Layer

Organised by four mandatory core subdirectories:

| Subdirectory | Responsibility |
| --- | --- |
| `persistent/` | Persistence concerns: repositories, database access, caching |
| `transport/` | Data transport: `transport/controller/` for REST controllers, `transport/messaging/` for messaging infrastructure, `transport/client/` for outbound clients (cloud SDKs, third-party APIs) kept behind usecase ports |
| `security/` | Any security-related infrastructure |
| `event/` | Cross-bounded-context event delivery: integration event publisher and listener |

- Each infra submodule has its own exception level to handle that submodule's infra-level exceptions.
- Infra depends only on domain, usecase, and shared modules.

### Worked Example

```text
reservation/
├── domain/
│   ├── Reservation.java
│   ├── ReservationId.java
│   └── exception/
│       └── ReservationAlreadySubmittedException.java
├── usecase/
│   ├── port/
│   │   ├── in/
│   │   │   ├── ReservationSubmitter.java
│   │   │   └── ReservationSubmittedIEHandler.java
│   │   └── out/
│   │       └── ReservationIEPublisher.java
│   ├── dto/
│   │   ├── SubmitReservationRequest.java
│   │   └── SubmitReservationResponse.java
│   ├── exception/
│   ├── SubmitReservation.java
│   └── HandleReservationSubmittedIE.java
└── infra/
    ├── persistent/
    ├── transport/controller/ReservationController.java
    ├── security/
    └── event/
        ├── SpringReservationIEPublisher.java
        └── SpringReservationIEListener.java

ie/
├── port/ReservationIE.java
└── events/ReservationSubmittedIE.java

kernel/
├── Money.java
└── Percentage.java
```

---

## 4. Encapsulation & The Gateway Rule

Each bounded context protects its internals and exposes a minimal, deliberate surface.

- **Inbound Gateway:** The `usecase/port/in/` interfaces are the only entry point into a bounded context's behavior. Callers (controllers, listeners) depend on the interface, never on the implementation.
- **Outbound Gateway:** Bounded contexts communicate with each other exclusively through integration events defined in the shared `ie/` module.
- **No Cross-Context Deep Importing:** A bounded context must never import domain, usecase, or infra classes from another bounded context. The only shared surface between contexts is `ie/`.
- **DTO Boundary:** A controller that depends on a usecase port keeps that port's dedicated request and response DTOs for OpenAPI. Domain objects are never exposed through transport.
- **Internal Independence:** Code inside a layer may reference its siblings freely. The rule governs cross-boundary traffic only.

---

## 5. Dependency Flow & Architecture Hierarchy Rules

Dependencies flow strictly inward. This prevents circular locks and keeps the business core free of technical concerns.

```text
[ INFRA ]    <-- May depend on usecase, domain, shared
    |
[ USECASE ]  <-- May depend on domain, shared
    |
[ DOMAIN ]   <-- May depend on kernel only (no usecase, infra, other shared, or framework)
    |
[ KERNEL ]   <-- Depends on nothing (Java standard library only)
```

### Direct Engineering Rules

1. **Domain Independence:** The domain module imports nothing from `usecase`, `infra`, or any shared module other than `kernel/`, and no framework types. `kernel/` itself imports nothing but the Java standard library.
2. **Usecase Direction:** The usecase layer points only toward domain and shared modules. Infrastructure is reached through `port/out` interfaces, never by direct reference.
3. **Infra Direction:** Infra implements the ports declared by the usecase layer and depends only on domain, usecase, and shared modules.
4. **Context Isolation:** Bounded contexts do not depend on each other. Shared contracts live in `ie/`.
5. **Runtime Isolation:** Cloud-provider SDKs and runtime-specific libraries are used only in `app/` and in the `infra` submodules that implement usecase ports. `domain` and `usecase` never import them.
6. **Automated Enforcement:** Dependency direction and runtime isolation are verified by tooling as a quality gate (see Section 9), not by code review alone.

---

## 6. Separation of Concerns (Definition vs. Implementation)

Intent is declared through interfaces and realized by implementations. The two never share a file.

### Usecase Discipline

- **One Usecase, One Service:** Every usecase has its own interface and implementation. God-like usecase services are prohibited.
- **Interface per Usecase:** The preferred convention is the implementation named for the action and the interface named for the actor of the action, e.g. `UserRegisterAccount implements UserAccountRegistrator`.
- **Intent-Revealing Methods:** Each interface method reflects its intent, e.g. `UserAccountRegistrator.register()`.
- **Dedicated DTOs:** Each interface has its own dedicated request and response data transfer objects. DTOs are not shared between usecases.

### Exception Discipline

Exceptions are owned by the layer that raises them:

- `domain/exception/` for business rule violations
- `usecase/exception/` for orchestration failures
- `infra/<submodule>/` exception level for technical failures of that submodule

### Domain Object Discipline

- Each domain object enforces its own business rule; rules are not pushed out to usecases or controllers.
- Naming follows the bounded context's business naming convention.

---

## 7. Domain-Driven Design Practices

### Entity Identity

Entity ids are generated through a type-safe, domain-owned generator following the pattern `EntityId().generate()`. The generator returns the id as a `String`, e.g. `ReservationId().generate()`.

### Integration Event Mechanism

Cross-bounded-context communication uses a shared integration event (IE) module, `ie/`, which other bounded contexts depend on.

| Component | Enforced Location | Naming Example |
| --- | --- | --- |
| Event interface | `ie/port` | `ReservationIE` |
| Events | `ie/events/` | `ReservationSubmittedIE implements ReservationIE` |
| Publisher interface | `usecase/port/out` | `ReservationIEPublisher` |
| Publisher implementation | `infra/event/` | `SpringReservationIEPublisher implements ReservationIEPublisher` |
| Usecase handler interface | `usecase/port/in` | `ReservationSubmittedIEHandler` |
| Usecase handler implementation | `usecase/` | `HandleReservationSubmittedIE implements ReservationSubmittedIEHandler` |
| Listener (interface & implementation) | `infra/event/` | `SpringReservationIEListener` |

#### Event Rules

- **Single Publisher:** Exactly one IE publisher component per bounded context, exposing `.publish()` that takes the IE interface from `ie/port`.
- **Listener Wiring:** The listener injects the IE usecase handler *interface*, never the implementation.
- **One Handler per Event:** Each event has its own usecase handler. A single god-like handler is prohibited.
- **Framework Delegation:** Both publisher and listener use the infra framework's event dispatching and transaction management, e.g. Spring's `ApplicationEventPublisher`, `@TransactionalEventListener`, and `@EventListener`.

---

## 8. Development Workflow

Module development is ordered by priority, always:

1. **Domain first:** model the business objects and their rules.
2. **Usecase second:** add the orchestrators, ports, and DTOs.
3. **Infra last:** implement persistence, transport, security, and events.

This order is enforced, not suggested. It keeps the business core free of infrastructure assumptions, because infra can only be written once the ports it must satisfy already exist.

---

## 9. Testing Discipline & Quality Gates

Testing is a core architectural invariant, and quality is verified deterministically after every module is developed.

### Testing Standards

- **Mandatory Prerequisite:** Robust, intent-focused unit tests must exist before any other gate is evaluated.
- **Behavioral Focus:** Tests validate business intent and input/output contracts, not implementation details.
- **Framework-Free Domain Tests:** Because the domain is framework-agnostic, domain tests run with no infrastructure framework.
- **Kernel Tests:** `kernel/` has its own unit tests and passes the same quality gates as any domain module.
- **Layer-Appropriate Tooling:** Use language-level and infra-level testing frameworks and analysis packages for each layer.
- **Shared Fixtures:** Test data builders and fixtures are captured in dedicated test-support files rather than duplicated inside test bodies.
- **Infra Integration Tests:** The `persistent` and `event` infra submodules are verified against local containers (e.g. Testcontainers, LocalStack), not mocks alone.

### Post-Development Quality Gates

Each gate is deterministic. Failing any gate fails the module.

| Gate | Threshold | On Failure |
| --- | --- | --- |
| Unit tests | Robust and intent-focused (mandatory prerequisite) | Fails |
| CRAP index | Below 6 | Fails |
| Mutation testing | Per layer, see Mutation Testing Thresholds below | Fails |
| Dependency rule compliance | Inward-only architecture; domain imports only `kernel/`; `kernel/` imports nothing | Fails |
| Startup budget | Within the agreed startup budget on the deployment target | Fails |

#### Mutation Testing Thresholds

| Layer | Threshold | Why |
| --- | --- | --- |
| `kernel/` and `domain` | At least 90% | Pure business logic with no framework or I/O, so nearly every mutant can be killed; the money and time rules live here |
| `usecase` | At least 80% | Mostly orchestration and port calls; some mutants only change wiring |
| `infra` | No mutation gate | Verified by integration tests against local containers instead; mutation runs here are slow and noisy |

- **Ratchet:** a module's mutation score may never drop below its last passing score, even while above the threshold.
- **Measure what is tested:** report both the overall mutation score and test strength (killed ÷ covered mutants), so untested code cannot hide behind the overall number.
- **Equivalent mutants:** 100% is not a target. A mutant that cannot change behaviour may be excluded only with a written reason next to the exclusion.

---

## 10. Deployment Discipline

Deployment is a delivery detail. Bounded-context modules are **runtime-agnostic**: nothing in `domain`, `usecase`, or a context's own `infra` may know where or how the application runs. The same build must run on a serverless function, a container platform, or a VM without code changes in any bounded context.

### Composition Root Rule

- `app/` is the only module that depends on every bounded context. It owns startup, the runtime entry point, and the wiring of infra implementations to usecase ports.
- No bounded context chooses its own runtime wiring or imports a runtime-specific or cloud-provider library, except inside an `infra` submodule that implements a usecase port.
- Changing the deployment target changes `app/` and `deploy/`, never a bounded context.

### Deployable Properties

Every deployable built from this codebase must satisfy these, whatever the target:

- **Stateless:** no in-memory sessions or caches that must survive between requests. State lives in persistence.
- **Configured from outside:** all environment-specific values and secrets are injected at startup from the environment or a managed store, never from code or the artifact.
- **Bounded startup:** startup time has an agreed budget, enforced as a quality gate. Initialisation is lazy where possible.
- **Disposable:** an instance can be started, stopped, or replaced at any moment without losing accepted work.
- **Connection-aware:** pool sizes are configurable per deployable, since instance count varies by target.
- **Observable:** logs go to standard output as structured records, and health and readiness are exposed through `transport/`.

### Deployment Granularity

- **Default:** one deployable runs all bounded contexts. In-process integration events keep working unchanged.
- **Split per bounded context** only when scaling, ownership, or fault isolation requires it. Cross-context events then move to a durable broker, confined to `infra/event/`. The `ie/` contracts, publisher interface, and handler interfaces stay as they are.
- Each deployable has its own entry module in `app/` that wires only its contexts plus shared `ie/`.

### Work Beyond a Request

- **Event reliability:** event handling is either synchronous within the transaction or goes through a durable outbox, never fire-and-forget threads. Instances may be stopped after a response.
- **Long-running or scheduled work:** triggered by a scheduler or queue. The trigger lands in `infra/transport/messaging/` and calls an ordinary usecase port.

### Edge Concerns

- **Authentication:** tokens may be verified at the edge (gateway, ingress, or proxy) or in the app. Either way `infra/security/` maps claims into the domain's terms.
- **Timeouts and limits:** the target's request limits are a design input. Usecases are sized to fit, or they hand work off as above.

### Build and Release

- Build once, deploy the same artifact to every environment.
- Infrastructure is defined as code in `deploy/<target>/`, versioned with the application.
- A new target is added by adding an entry in `app/` and a `deploy/<target>/` folder.

### Target Adapters (Reference)

| Target | Lives in `app/` | Lives in `deploy/` |
| --- | --- | --- |
| Serverless function | Function handler adapter, lazy init | Function, gateway, alias definitions |
| Container platform | Standard server entry point | Image build, service, scaling and probe definitions |
| VM | Standard server entry point | Provisioning and service unit definitions |

---

## 11. Technology Stack

The principles above are stack-independent. The reference implementation is:

- **Language:** Java
- **Infrastructure framework:** Spring (REST controllers, event dispatching, transaction management)
- **Deployment target:** AWS Lambda behind API Gateway (the discipline in Section 10 is target-agnostic)
