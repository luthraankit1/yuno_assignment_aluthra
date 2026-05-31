# Payment Orchestration Service — Requirements

This document defines functional and non-functional requirements for the **payment-orchestration** application (`com.yuno.assignment`). It aligns with the implemented API, routing, provider failover, and idempotency behavior.

---

## 1. Functional requirements

### 1.1 Payment creation (FR-001)

| ID | Requirement |
|----|-------------|
| FR-001.1 | The system SHALL expose `POST /api/v1/payments` to initiate a payment. |
| FR-001.2 | The request body SHALL include `customerId`, `amount`, `currency`, `country`, and `paymentMethodToken`. |
| FR-001.3 | On successful provider charge, the API SHALL respond with HTTP **201 Created** and a payment payload whose `status` is `SUCCEEDED`. |
| FR-001.4 | When all providers in the route fail, the API SHALL respond with HTTP **200 OK** and `status` **FAILED** (payment recorded, not charged successfully). |

### 1.2 Request validation (FR-002)

| ID | Requirement |
|----|-------------|
| FR-002.1 | `customerId` SHALL be non-blank, length 10–64. |
| FR-002.2 | `amount` SHALL be at least **1.00**. |
| FR-002.3 | `currency` and `country` SHALL be 3-letter uppercase ISO-style codes (`^[A-Z]{3}$`). |
| FR-002.4 | `paymentMethodToken` SHALL be non-blank, max length 128. |
| FR-002.5 | Validation failures SHALL return HTTP **400** with error code `validation_failed` and field-level details. |

### 1.3 Idempotency (FR-003)

| ID | Requirement |
|----|-------------|
| FR-003.1 | Clients SHALL send header `PAYMENT-IDENTIFIER` with a unique key per logical payment attempt. |
| FR-003.2 | Missing header SHALL yield HTTP **400** (`missing_header`). |
| FR-003.3 | First request with a key SHALL reserve the key, process the payment, and cache the serialized response. |
| FR-003.4 | Subsequent requests with the same key (after completion) SHALL return the cached response without re-processing. |
| FR-003.5 | Concurrent duplicate requests while processing SHALL receive HTTP **409** (`in_flight`). |
| FR-003.6 | On processing failure after reservation, the in-flight marker SHALL be released so the client may retry. |

### 1.4 Provider routing (FR-004)

| ID | Requirement |
|----|-------------|
| FR-004.1 | Routing rules SHALL be configurable (JSON) with optional filters: `currency`, `country`, `amountMax`. |
| FR-004.2 | The engine SHALL apply the **first** rule that matches the payment. |
| FR-004.3 | If no rule matches, the system SHALL use configured **default** providers (ordered failover list). |
| FR-004.4 | If no providers are resolved, the payment SHALL be marked failed and HTTP **422** returned (`unprocessable_entity`). |

### 1.5 Provider execution and failover (FR-005)

| ID | Requirement |
|----|-------------|
| FR-005.1 | For each payment, the system SHALL try providers in route order until one succeeds. |
| FR-005.2 | Each provider attempt SHALL be persisted (`PaymentAttempt`) with attempt number, provider id, status, and error details on failure. |
| FR-005.3 | Provider runtime exceptions SHALL be treated as a failed attempt (`PROVIDER_EXCEPTION`) and SHALL trigger failover when more providers remain. |
| FR-005.4 | On success, the payment SHALL store `selectedProvider` and `providerReference`. |
| FR-005.5 | On total failure, the payment SHALL store `failureReason` from the last provider error. |

### 1.6 Persistence (FR-006)

| ID | Requirement |
|----|-------------|
| FR-006.1 | Payments and attempts SHALL be stored in a relational database. |
| FR-006.2 | Payment lifecycle statuses: `INITIATED`, `IN_PROGRESS`, `SUCCEEDED`, `FAILED`. |
| FR-006.3 | Timestamps `createdAt` and `updatedAt` SHALL be maintained on payments. |

### 1.7 Error handling (FR-007)

| ID | Requirement |
|----|-------------|
| FR-007.1 | API errors SHALL use a consistent `ApiError` structure: `timestamp`, `status`, `error`, `message`, `details`. |
| FR-007.2 | Unhandled server errors SHALL return HTTP **500** (`internal_error`) without leaking stack traces to clients. |

---

## 2. Non-functional requirements

### 2.1 Performance (NFR-001)

| ID | Requirement |
|----|-------------|
| NFR-001.1 | Provider connectors MAY simulate latency per provider configuration; production targets SHOULD keep p95 API latency within agreed SLO (e.g. &lt; 2s excluding external PSP latency). |
| NFR-001.2 | Idempotency lookups SHOULD complete in sub-millisecond range when Redis is co-located. |

### 2.2 Availability and reliability (NFR-002)

| ID | Requirement |
|----|-------------|
| NFR-002.1 | Provider failover SHALL improve success rate when a primary PSP is unavailable or declines. |
| NFR-002.2 | Idempotency SHALL prevent duplicate charges for retried client requests with the same key. |
| NFR-002.3 | The service SHOULD tolerate Redis transient failures via retry semantics on idempotency reservation (bounded retries). |

### 2.3 Scalability (NFR-003)

| ID | Requirement |
|----|-------------|
| NFR-003.1 | Application instances SHALL be stateless; shared state SHALL live in SQL Server (or H2 in tests) and Redis. |
| NFR-003.2 | Routing configuration SHALL be reloadable on application restart via properties (no code deploy for rule changes). |

### 2.4 Security (NFR-004)

| ID | Requirement |
|----|-------------|
| NFR-004.1 | Payment method tokens SHALL NOT be logged at INFO level in production. |
| NFR-004.2 | Database and Redis credentials SHALL be supplied via environment or secrets management, not committed to source control. |
| NFR-004.3 | API SHOULD be placed behind TLS termination and authentication in production (out of scope for local assignment build). |

### 2.5 Observability (NFR-005)

| ID | Requirement |
|----|-------------|
| NFR-005.1 | Structured logs SHALL record payment id, provider id, routing decisions (DEBUG), and idempotency replay (INFO). |
| NFR-005.2 | Each `PaymentAttempt` SHALL provide an audit trail for compliance and support. |

### 2.6 Maintainability and testability (NFR-006)

| ID | Requirement |
|----|-------------|
| NFR-006.1 | Core logic (routing rules, payment orchestration, idempotency outcomes) SHALL be covered by automated JUnit 5 tests. |
| NFR-006.2 | Test suites SHALL be categorized: **unit**, **sanity**, **integration**, **regression** (JUnit `@Tag`). |
| NFR-006.3 | Integration tests SHALL run without external SQL Server or Redis (H2 + in-memory idempotency). |

### 2.7 Compatibility (NFR-007)

| ID | Requirement |
|----|-------------|
| NFR-007.1 | Runtime: Java 8+, Spring Boot 2.7.x. |
| NFR-007.2 | JSON API compatible with Jackson default property naming. |

---

## 3. Test strategy mapping

| Test type | Purpose | Location / tag |
|-----------|---------|----------------|
| **Sanity** | Fast smoke checks that API wiring and validation work | `sanity.*`, `@Tag("sanity")` |
| **Unit** | Isolated logic: routing rules, service, idempotency | `unit.*`, `@Tag("unit")` |
| **Integration** | `PaymentService` + JPA + routing + providers (H2); HTTP omitted where `@Valid` on `PaymentRequest` blocks the API | `integration.*`, `@Tag("integration")` |
| **Regression** | Guards against known bugs (boundaries, audit, defaults) | `regression.*`, `@Tag("regression")` |


---

## 4. Out of scope / Future improvements

- Real PSP integrations (mock connectors only)
- Authentication / authorization
- Webhooks and settlement reconciliation