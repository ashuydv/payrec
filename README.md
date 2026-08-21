# PayRecon

A payment reconciliation and processing platform: it ingests payment transactions, processes them, and reconciles them nightly against a simulated bank feed to catch mismatches, missing transactions, and duplicate charges. Built as a portfolio project to demonstrate a production-shaped Java/Spring Boot + Angular stack — layered architecture, idempotency, concurrency safety, batch processing, and a working dashboard, not a tutorial CRUD app.

## Architecture

```
┌─────────────────┐        ┌──────────────────────────────────────────┐        ┌────────────┐
│   Angular SPA    │  HTTP  │              Spring Boot API              │  JDBC  │  Postgres  │
│  (Material UI)   │───────▶│  Controller → Service → Repository        │───────▶│            │
└─────────────────┘        │  DTOs at the boundary, JPA entities inside │        └────────────┘
                            │  Spring Batch job for nightly reconciliation
                            └──────────────────────────────────────────┘
```

- **Backend**: Java 17, Spring Boot 3.3, Spring Data JPA, Spring Batch, Spring Validation. Maven (with the wrapper committed, so no local Maven install is needed).
- **Frontend**: Angular 22, standalone components (no NgModules), Angular Material.
- **Database**: PostgreSQL.
- **Infra**: Docker Compose for local multi-service runs; a Jenkinsfile for CI.

### Layering

Every backend feature follows the same shape: `Controller` (HTTP concerns only) → `Service` interface + impl (business rules, transactions) → `Repository` (Spring Data JPA). DTOs are the only thing that crosses the controller boundary — JPA entities never get serialized directly to JSON, so persistence concerns (lazy loading, `@Version`, bidirectional associations) can't leak into the API contract.

## Design patterns used, and why

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `ReconciliationRule` (`ExactMatchRule`, `TolerantMatchRule`) | Swap matching strictness (exact vs. tolerance-band) without touching `ReconciliationService`. |
| **Strategy** | `PaymentProcessor` (`CardPaymentProcessor`, `BankTransferPaymentProcessor`) | Each payment rail's processing logic is isolated and independently testable. |
| **Factory** | `PaymentProcessorFactory` | Spring collects every `PaymentProcessor` bean and indexes it by `PaymentType`; adding a new payment rail is just adding a new `@Component`, no factory code changes. |
| **Idempotent create via DB constraint** | `TransactionInserter`, `SettlementUpserter` | Rather than "check-then-insert" in application code (which races under concurrency), the database's unique constraint is the source of truth: optimistically insert, and on a constraint violation, re-fetch and return the winner. See the comment block atop `TransactionServiceImpl`. |
| **Optimistic locking** | `Settlement.version` (`@Version`) | Prevents two concurrent settlement runs for the same merchant/period from corrupting the total — proven by a real multi-threaded test (`SettlementServiceImplConcurrencyTest`) that fires 8 threads at the same settlement and asserts the final total is exactly correct. |

## Key engineering decisions

- **Idempotency**: `POST /api/transactions` is idempotent on `externalReference`. A retried request (e.g. after a client timeout) returns the original transaction with `200 OK` instead of creating a duplicate or erroring — enforced at the database level, not just in application code, so it holds under real concurrency.
- **Concurrency safety**: Settlement totals use optimistic locking with a bounded retry (3 attempts) before surfacing a `409 Conflict` — chosen deliberately over retrying forever, so a genuinely stuck contention scenario is visible to the caller rather than silently hanging.
- **Batch fault tolerance**: The nightly reconciliation job (Spring Batch, chunk size 50) uses a skip policy — up to 20 bad records are logged and skipped without failing the whole run, but exceeding that limit still fails the job loudly, on the theory that dozens of failures signals a systemic problem (e.g. the bank feed didn't load), not a one-off bad row.
- **Structured errors**: Every exception maps to a JSON error body via `@RestControllerAdvice` — `{timestamp, status, error, message, path, fieldErrors}` — never a raw stack trace.

## Simplifying assumptions (what's genuinely demo-grade vs. production-grade)

- Schema management uses Hibernate's `ddl-auto: update` rather than versioned migrations (Flyway/Liquibase). Fine for a demo; a real service would need migrations for safe rollout.
- No authentication/authorization anywhere in the API.
- The "bank feed" is simulated as `LedgerEntry` rows with `source = BANK_FEED` already sitting in the database, rather than an actual file/SFTP/webhook ingestion pipeline.
- Payment processors (`CardPaymentProcessor`, `BankTransferPaymentProcessor`) always simulate success — no real gateway integration.
- The Jenkinsfile's deploy stage is simulated (echoes what it would do) rather than pushing to a real registry/cluster.

## Running locally

### Option A — Docker Compose (everything)

```bash
docker compose up --build
```

- Frontend: http://localhost:4200
- Backend API: http://localhost:8080/api
- Postgres: localhost:5435 (mapped from the container's 5432, to avoid clashing with other local Postgres instances)

### Option B — Run services individually (faster feedback loop while developing)

```bash
# 1. Start just Postgres
docker compose up -d postgres

# 2. Backend (uses the Maven wrapper, no local Maven install needed)
cd backend
./mvnw spring-boot:run

# 3. Frontend, in a second terminal
cd frontend
npm install
npx ng serve
```

Then visit http://localhost:4200. The Angular dev server calls `http://localhost:8080/api` directly (see `src/environments/environment.ts`); the backend's `WebConfig` allows that cross-origin call in dev. In the Docker Compose setup, nginx proxies `/api` to the backend container instead, so the browser only ever talks to one origin.

## Tests

```bash
# Backend: JUnit 5 + Mockito unit tests, plus Spring Batch/Boot integration tests
cd backend && ./mvnw test

# Frontend: component + service tests (Vitest, Angular's current default test runner)
cd frontend && npx ng test --watch=false
```

## Project layout

```
backend/    Spring Boot API — see backend/src/main/java/com/payrecon/
  domain/          JPA entities + enums
  dto/             Request/response records (never expose entities directly)
  repository/      Spring Data JPA repositories
  service/         Transaction CRUD + idempotency
  reconciliation/  Strategy-pattern matching rules + reconciliation service
  processing/      Factory-pattern payment processor dispatch
  retry/           Retry policy + service for FAILED transactions
  settlement/      Settlement aggregation with optimistic-lock concurrency handling
  batch/           Spring Batch nightly reconciliation job
  controller/      REST endpoints
  exception/       Custom exceptions + global @RestControllerAdvice handler
  config/          CORS and other cross-cutting config

frontend/   Angular 22 standalone-component SPA — see frontend/src/app/
  core/            API models + HttpClient services
  features/        Route-level pages (transactions, reconciliation, settlements)
  shared/layout/   App shell + navigation

docker-compose.yml   Postgres + backend + frontend, wired together
Jenkinsfile          CI pipeline: checkout → build/test → docker build → deploy
```
