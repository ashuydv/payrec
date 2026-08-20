# Phase 6 — Scheduled settlement runs

## What

`SettlementScheduler` fires once a day (`payrecon.settlement.schedule.cron`,
default `0 0 2 * * *` — 02:00 UTC) and runs settlement for the *previous*
UTC day, so nobody has to remember to `POST /api/settlement-runs` by hand.
It settles yesterday rather than today on the assumption that a day's
transactions need a full day to arrive and reconcile before it makes sense
to settle them — running mid-day would just settle a still-in-progress day
and need a rerun anyway (safe, since reruns are idempotent, but wasteful).

The launch-a-job-and-extract-its-result logic that used to live directly in
`SettlementRunController` moved out into `SettlementRunLauncher`
(`batch` package), so the HTTP-triggered path and the cron-triggered path
share the exact same launch/status-check/result-extraction rather than two
copies drifting apart. The controller is now a thin wrapper over it.

Gated by `payrecon.settlement.schedule.enabled` (`ConditionalOnProperty`,
default `true`, set `false` in `application-test.yml`) so the scheduler bean
doesn't even get registered during tests — no risk of a cron firing mid
test-run and racing `SettlementBatchIntegrationTest`'s own job launches. A
failed scheduled run is caught and logged, not allowed to propagate: one bad
night's run (a transient DB blip, say) shouldn't be fatal when tomorrow's
firing — or a manual trigger — can just retry the same period.

## Why

Phase 4 built the settlement batch job; Phase 5 made its output visible and
closeable. Both assumed a human (or an external cron) calls
`POST /api/settlement-runs` on a schedule. That's a real operational gap for
software whose whole point is periodic settlement — "someone has to
remember to run it" is exactly the kind of manual step this system exists to
remove. Building the schedule in-process (Spring's `@Scheduled`) rather than
depending on an external scheduler keeps the app self-contained, consistent
with `docker-compose.yml` being the only infra dependency so far.

## Not in scope here

- **Single-instance assumption.** `@Scheduled` fires in every running
  instance; running more than one app instance would fire the job
  redundantly (harmless here, since `runSettlement` is idempotent per
  period and concurrent runs just show up as `merchantsSkippedConflict` on
  `Settlement.version`'s optimistic lock — but wasteful). A distributed
  lock (Spring Integration's `LockRegistry`, ShedLock, or Spring Batch's own
  instance-affinity options) would be the fix once there's more than one
  instance to worry about.
- **No retry/backoff beyond "wait for tomorrow."** A failed run just logs
  and waits for the next scheduled firing (or a manual trigger); there's no
  automatic same-day retry.
- **No visibility into scheduled-run history beyond logs.** Nothing
  persists "the 2026-08-19 run happened at 02:00 and settled 4 merchants" —
  that's implicit in Spring Batch's own `JobExecution` history (queryable
  via `JobExplorer` if this becomes a real need) plus whatever log
  aggregation the deployment has.
