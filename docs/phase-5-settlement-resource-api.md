# Phase 5 — Settlement resource API

## What

`Settlement` rows have existed since Phase 1 and get created/updated by
Phase 4's batch job, but until now there was no way to look at one or
finalize it — only to trigger the run that produces them. This phase adds
the missing resource endpoints:

- `GET /api/settlements/{id}` — fetch one settlement
- `GET /api/settlements?merchantId=&status=` — paged list, optionally
  filtered by merchant and/or `SettlementStatus` (`SettlementSpecifications`,
  mirroring `TransactionSpecifications` from Phase 1)
- `PATCH /api/settlements/{id}/status` — transition status, body
  `{"status": "FINALIZED"}` (mirrors `PATCH /api/transactions/{id}/status`
  from Phase 1: same `UpdateXStatusRequest` shape, same
  `InvalidStatusTransitionException` on a disallowed move)

Only `OPEN -> FINALIZED` is a real transition; `FINALIZED -> FINALIZED` is
accepted as a no-op (idempotent retries shouldn't fail), and everything else
(`FINALIZED -> OPEN`, or any no-op that isn't already `FINALIZED -> FINALIZED`
handled by the equality check) is rejected. There's no un-finalize — a
finalized settlement is meant to be the closed, paid-out record; reopening
one is a business decision this API doesn't make for you.

The existing batch-trigger controller (`POST /api/settlement-runs`) was
renamed from `SettlementController` to `SettlementRunController` — a pure
internal rename, its endpoint path is unchanged — so the more natural name,
`SettlementController`, could go to the new `/api/settlements` resource
controller instead of colliding with it.

## Why

A settlement that can be created (Phase 4) but never viewed or closed out
isn't useful yet — something (a finance team, a payout export job, an
operator) needs to see what a batch run produced and mark it as paid.
`FINALIZED` already existed as a `SettlementStatus` value and the batch job
already respected it (skipping finalized settlements rather than reopening
them), but nothing could actually *set* it. This phase closes that loop: it's
the last piece needed to call the transaction → reconciliation → settlement
pipeline actually usable end-to-end, not just internally consistent.

## Not in scope here

- **No un-finalize / reopen.** If a finalized settlement turns out to be
  wrong, the correct move isn't obvious yet (adjust in place? void and
  recreate? emit a correcting settlement?) — deferred until there's a real
  case to design against.
- **No payout/export integration.** Finalizing a settlement doesn't trigger
  anything downstream (a bank transfer, a merchant notification). This API
  only tracks the settlement's own state.
