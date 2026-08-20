# Phase 4 — Settlement batch job

## What

`POST /api/settlement-runs?period=2026-08-19` launches a Spring Batch job
that aggregates one day's reconciled transactions into per-merchant
`Settlement` rows, and blocks until it finishes:

1. **Candidate selection.** `TransactionRepository` finds every `PROCESSED`
   transaction with `processedAt` inside the UTC day named by `period` that
   isn't attached to a settlement yet (`Transaction.settlement IS NULL`).
2. **Grouping.** Candidates are grouped by merchant.
3. **Per merchant (`SettlementMerchantRunner`, its own `REQUIRES_NEW`
   transaction — same reasoning as Phase 1's `TransactionInserter`):**
   classify each candidate with the same `ReconciliationClassifier` the
   on-demand reconciliation endpoint uses (Phase 3), and sum only the ones
   that come back `MATCHED`. Find-or-create that merchant's `Settlement` row
   for the period, add the matched total to it, and attach it to each
   matched transaction (`Transaction.settlement`). A `FINALIZED` settlement
   is left alone — its transactions stay unsettled rather than getting
   silently reopened.
4. **Isolation.** One merchant's `ObjectOptimisticLockingFailureException`
   — `Settlement.version` catching a concurrent settlement run against the
   same merchant/period — is caught and counted, not allowed to roll back
   every other merchant already processed in this run.

Wired as an actual Spring Batch `Job`/`Step` (`SettlementBatchConfig`,
`SettlementTasklet`) rather than calling the service directly from the
controller, since `spring-boot-starter-batch` was already a dependency and
`spring.batch.job.enabled: false` in `application.yml` was already disabling
Boot's "run every Job on startup" behavior — both clearly staged for this.
A `Tasklet`, not a chunk-oriented reader/processor/writer step: the work is
"group, aggregate, upsert" as one cohesive unit, not an item-at-a-time
stream, so chunk restartability wouldn't add anything. Each trigger adds a
`startedAt` job parameter so the same `period` can be safely relaunched —
Spring Batch treats identical `JobParameters` as the same instance and
refuses to rerun a completed one, and rerunning a period is exactly what a
"run it again once more transactions have reconciled" workflow needs.

## Why

Phase 3 gave every transaction a trustworthy MATCHED/mismatched verdict.
Settlement is where that verdict turns into money owed: it aggregates a
merchant's confirmed transactions into the periodic payout total
(`Settlement.totalAmount`) that the product exists to produce.
`Settlement.version`, `@Version` optimistic locking, already carried a
comment saying it existed for exactly this phase, to stop two concurrent
settlement runs for the same merchant/period from double-counting
transactions — this phase is what that comment was waiting for.

`Transaction.settlement` (new nullable FK) is what makes settlement runs
idempotent. Without it, rerunning a period would re-sum transactions that
were already counted into a prior run's total. With it, the candidate query
naturally excludes anything already settled, so a rerun only ever picks up
transactions that are new to the window or have since become MATCHED — no
separate "already processed" bookkeeping needed, and it gives free
traceability (which settlement a transaction ended up in).

`ReconciliationClassifier` (extracted from `ReconciliationServiceImpl`,
`service/impl` package) exists so both the HTTP reconciliation endpoint and
this batch apply the exact same MATCHED test — a transaction can't be
"matched" through one path and "not matched" through the other.

## Not in scope here

- **No scheduling.** The endpoint is synchronous and manually triggered.
  Running it automatically (nightly cron, `@Scheduled`, or a
  `create_trigger`-style external scheduler) is an operational decision for
  later, not a code gap — the job itself is what needed building first.
- **No re-finalization workflow.** `SettlementStatus.FINALIZED` is honored
  (skipped, not reopened) but nothing in this codebase yet transitions a
  settlement from `OPEN` to `FINALIZED`, or exposes settlements over REST at
  all. Both are natural next steps once there's a consumer that needs them
  (e.g. a payout export).
- **Single currency assumed.** `Settlement.totalAmount` has no currency
  field — a pre-existing simplification from the Phase 1 domain model, not
  something introduced here. Fine as long as a merchant only transacts in
  one currency; revisit if that stops being true.
- **Bad `period` input surfaces as a generic 500.** `LocalDate.parse`
  failures happen inside the Tasklet, so the job fails rather than the
  controller returning a clean 400. Acceptable for an ops-facing trigger
  endpoint for now; worth tightening if this becomes user-facing.
