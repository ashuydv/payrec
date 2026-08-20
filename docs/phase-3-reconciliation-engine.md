# Phase 3 — Reconciliation engine

## What

`GET /api/transactions/{id}/reconciliation` — for one transaction, fetches
every `LedgerEntry` recorded against its `externalReference` and classifies
the result as a `ReconciliationStatus`:

| Status                      | Meaning                                                                 |
|------------------------------|--------------------------------------------------------------------------|
| `NO_LEDGER_ENTRIES`          | Nothing has been ingested for this reference yet.                      |
| `AMOUNT_MISMATCH`             | At least one entry's `recordedAmount` disagrees with the transaction.  |
| `MISSING_BANK_CONFIRMATION`   | Entries agree on amount, but none is a `BANK_FEED` entry yet.          |
| `MATCHED`                     | Entries agree on amount and include both `INTERNAL` and `BANK_FEED`.   |

Implemented in `ReconciliationServiceImpl.classify`: amounts are compared
with `BigDecimal.compareTo`, not `equals`, since `10.0` and `10.00` are the
same money but different scale and `equals` would wrongly call that a
mismatch.

The result (`ReconciliationResponse`) is computed on request, not persisted —
there is no new table or column here. `ReconciliationStatus` is a pure
function of existing `Transaction` + `LedgerEntry` rows, so persisting it
would just be a cache that goes stale the moment a new ledger entry arrives.

## Why

This is the core of what "PayRecon" does: turn two independently-arriving,
unordered event streams (our own processing, the bank's feed) into a
trustworthy verdict about whether a transaction is confirmed. Phase 1 gave
us transactions, Phase 2 gave us a way to ingest the ledger entries that
confirm them; Phase 3 is what actually connects the two into the
reconciliation the product is named for.

It's also the gate for Phase 4. Settlement should only aggregate
transactions that are actually confirmed by the bank — rolling up a
transaction that's `AMOUNT_MISMATCH` or still `MISSING_BANK_CONFIRMATION`
into a settlement would settle money that was never actually verified.
Phase 4's batch job is expected to call this same classification (directly,
not through HTTP) as its filter for "is this transaction eligible to settle."

## Not in scope here

- **Batch/bulk reconciliation.** Only a per-transaction endpoint exists.
  Reconciling "all pending transactions" is what Phase 4's batch job needs,
  and it will call the classification logic directly rather than loop over
  this HTTP endpoint — a REST API given a giant merchant's queue is the
  wrong integration point.
- **Orphan ledger entries** (entries with a `transactionReference` that
  matches no transaction's `externalReference`) aren't surfaced anywhere
  yet. Detecting them needs a different access pattern — scanning
  `LedgerEntry` for references absent from `Transaction`, versus this
  phase's transaction-first lookup — and nothing downstream needs it yet.
  Worth adding once there's a reporting/dashboard consumer.
- **No status field on `Transaction`.** Deliberately not storing
  reconciliation status back onto the transaction row (e.g. as a new
  `Transaction.reconciliationStatus` column) — it would need explicit
  invalidation every time a ledger entry lands, and get out of sync the
  moment that's missed. Computing on read avoids that whole class of bug
  until there's a proven need to cache it.
