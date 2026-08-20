# Phase 2 — Merchant management & ledger ingestion API

## What

Two new REST surfaces, layered the same way Phase 1's transaction API is
(`controller` → `service` → `repository`, DTOs kept separate from JPA
entities):

**Merchant API** — `MerchantController` / `MerchantService`
- `POST /api/merchants` — create a merchant (`name`, `settlementAccount`)
- `GET /api/merchants/{id}` — fetch one
- `GET /api/merchants` — paged list
- `PUT /api/merchants/{id}` — update `name` / `settlementAccount`

**Ledger entry ingestion API** — `LedgerEntryController` / `LedgerEntryService`
- `POST /api/ledger-entries` — record a ledger entry (`transactionReference`,
  `recordedAmount`, `source` — `INTERNAL` or `BANK_FEED`)
- `GET /api/ledger-entries?transactionReference=...` — list all entries
  recorded against a given transaction reference

Both follow existing conventions: bean-validated request DTOs, `ErrorResponse`
via the shared `GlobalExceptionHandler`, `ResourceNotFoundException` for
missing rows, Mockito-based service unit tests mirroring
`TransactionServiceImplTest`'s style (`@ExtendWith(MockitoExtension.class)`,
reflection-set ids since entities use DB-generated identity).

## Why

**Merchant API.** `TransactionController.createTransaction` already required
an existing `merchantId` (Phase 1), but no endpoint could create one —
merchants had to be inserted directly into the database. This was the
sharpest gap blocking any real use of the transaction API.

**Ledger entry ingestion.** `LedgerEntry` (domain, Phase 1) models a claim
from one of two independent sources about a transaction: our own internal
processing pipeline, or the bank's settlement/statement feed. Phase 3's
reconciliation engine will match transactions against these entries by
`externalReference` / `transactionReference` and needs a `transactionReference`
lookup — which `LedgerEntryRepository.findByTransactionReference` already
supported; only the intake and read endpoints were missing. There is
deliberately no update or delete endpoint: a ledger entry is a historical
record of what a source reported at ingestion time. If a `BANK_FEED` entry
turns out to disagree with the internal record, that disagreement is exactly
what reconciliation is for — it becomes a discrepancy to resolve, not a row
to silently overwrite. Losing that append-only property would erase the
audit trail reconciliation depends on.

## Not in scope here

- No idempotency guard on ledger entry ingestion (unlike transaction
  creation). Bank feeds are expected to be de-duplicated by reconciliation
  logic in Phase 3, which will see repeated entries as corroborating
  evidence rather than as errors; revisit if a source's replay behavior
  turns out to need stronger guarantees.
- No merchant deletion — merchants are referenced by transactions and (from
  Phase 4) settlements, so removing one needs a decision about what happens
  to its history. Out of scope until something actually needs it.
