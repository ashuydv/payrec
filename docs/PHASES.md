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

## Phase 3 — Reconciliation engine (planned)

Matches each transaction against its ledger entries (by `externalReference`)
and classifies the result: matched (amounts agree across sources), amount
mismatch, missing bank confirmation, or an orphaned ledger entry with no
transaction. This is the core "reconciliation" in PayRecon — it turns two
independent, unordered event streams into a per-transaction verdict that
downstream settlement can trust.

## Phase 4 — Settlement batch job (planned)

Aggregates reconciled transactions into per-merchant, per-period
`Settlement` rows via Spring Batch (already a dependency; batch auto-run is
disabled in `application.yml` pending this phase). `Settlement.version`
already carries a `@Version` optimistic lock — noted in the entity as
existing for this phase — to stop two concurrent settlement runs for the
same merchant/period from double-counting transactions.
