# PayRecon build phases

PayRecon reconciles payment transactions against the ledger records that
confirm them (our own internal ledger, and the bank's), then rolls
reconciled transactions up into per-merchant settlements. The phases below
build that pipeline in order: each one produces the data a later phase needs
to consume. This file tracks *what* each phase built and *why*, so intent
survives past the PR description. Individual phases get their own doc under
`docs/` when there's enough nuance to explain (idempotency strategy, matching
rules, batch design, etc).

## Phase 1 — Core domain, REST API, idempotent transaction creation ✅

Established the domain model (`Merchant`, `Transaction`, `Settlement`,
`LedgerEntry`), the layered architecture (controller / service / repository,
DTOs separate from JPA entities), global exception handling, and the first
real endpoint: transaction creation. Transaction creation is idempotent on
`externalReference`, backed by a DB unique constraint plus an
insert-and-recover pattern rather than a racy check-then-insert — the
upstream payment feed can safely retry a POST without producing duplicate
charge records. See `TransactionServiceImpl` / `TransactionInserter`.

## Phase 2 — Merchant management & ledger ingestion API ✅

Two prerequisites reconciliation can't run without:

- **Merchant API.** Transaction creation already requires an existing
  `merchantId`, but nothing could create one — merchants had to be inserted
  by hand. Adds create/get/list/update endpoints.
- **Ledger entry ingestion API.** `LedgerEntry` rows are the raw evidence
  that a transaction actually happened — one stream from our own internal
  processing (`INTERNAL`), one from the bank's statement feed (`BANK_FEED`).
  Reconciliation (Phase 3) matches these against transactions by
  `externalReference`. Ingestion is append-only: a ledger entry is a
  historical record of what a source reported, not a mutable row we correct
  in place — a wrong entry gets reconciled as a discrepancy, not edited away.

See `docs/phase-2-merchant-and-ledger-ingestion.md`.

## Phase 3 — Reconciliation engine ✅

Matches each transaction against its ledger entries (by `externalReference`)
and classifies the result: `MATCHED` (amounts agree across an `INTERNAL` and
a `BANK_FEED` entry), `AMOUNT_MISMATCH`, `MISSING_BANK_CONFIRMATION`, or
`NO_LEDGER_ENTRIES`. Computed on demand via
`GET /api/transactions/{id}/reconciliation` rather than persisted — see
`ReconciliationServiceImpl`. This is the core "reconciliation" in PayRecon —
it turns two independent, unordered event streams into a per-transaction
verdict that downstream settlement can trust.

See `docs/phase-3-reconciliation-engine.md`.

## Phase 4 — Settlement batch job ✅

Aggregates reconciled (Phase 3 MATCHED) transactions into per-merchant,
per-period `Settlement` rows via an actual Spring Batch `Job`/`Step`,
triggered on demand through `POST /api/settlement-runs?period=...`.
`Settlement.version`'s `@Version` optimistic lock — noted in the entity as
existing for this phase — now does its job: a concurrent settlement run for
the same merchant/period is caught and skipped rather than double-counting.
A new nullable `Transaction.settlement` FK marks which transactions have
already been aggregated, which is what makes rerunning a period safe.

See `docs/phase-4-settlement-batch.md`.

## What's next (not yet planned in detail)

The domain now supports the full transaction → reconciliation → settlement
pipeline. Natural follow-ups, none started yet: exposing `Settlement` over
REST (list/get, and a `FINALIZED` transition), scheduling settlement runs
instead of triggering them by hand, and surfacing orphaned ledger entries
(see `docs/phase-3-reconciliation-engine.md`'s "not in scope").
