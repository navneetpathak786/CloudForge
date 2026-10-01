# CloudForge — Architecture (V1)

This document describes the proposed V1 architecture based on `docs/vision.md` and `docs/requirements.md`. It covers the minimal components, communication patterns, job lifecycle, and key design decisions for the first working version of the system.

---

## 1. Components and Responsibilities

| Component | Responsibility |
|---|---|
| **API Service** | Accepts job submission/cancellation/query requests, validates input, writes new jobs to the Job Store as `SUBMITTED`, enqueues them. |
| **Job Store** | Durable, authoritative record of all job state, worker state, and execution history. The single source of truth — all other components derive state from it. |
| **Queue** | Durable staging area holding `QUEUED` job IDs between submission and scheduling. |
| **Scheduler** | Pulls queued jobs, filters eligible/healthy workers, applies a scheduling strategy, reserves resources, assigns jobs, records the decision. Strategy is pluggable — different strategies can be swapped in for comparison. Single instance in V1. |
| **Worker Registry** | Tracks each worker's capacity, current usage, status, last heartbeat, and running jobs. Backed by the Job Store. |
| **Health Monitor** | Background loop (inside the Scheduler process) that detects stale heartbeats, marks workers `DOWN`, and triggers rescheduling of their jobs. On Scheduler restart, it re-checks all workers' heartbeats before scheduling resumes, so no stale-dead worker is treated as healthy. |
| **Worker Agent** | Registers with CloudForge, sends heartbeats, receives assignments, executes jobs in isolated containers, reports results, releases resources. V1 captures stdout/stderr only, size-limited; input data and output artifact handling are deferred. |
| **Metrics/Events** | Derived from job and worker state transitions for most counters/timers. Metrics that reflect a point-in-time condition (e.g., worker resource utilization, scheduler decision latency) require periodic sampling in addition to transition events. Feeds dashboards and experiment measurements; not a separately tracked source of truth. |
| **Experiment Runner** | Runs a defined set of jobs under a chosen scheduling strategy, collects the resulting metrics, and records results so different strategies can be compared reproducibly (per `docs/requirements.md` §10). |

These are logical components — several may run as a single deployable process in V1 (e.g., Scheduler + Health Monitor).

---

## 2. Communication and Data Flow

- **State store is authoritative; queues/messages are delivery mechanisms.** Any state change is written to the Job Store first; messages exist to trigger action, not to hold truth.
- **API → Queue:** job is written to the store as `SUBMITTED` before being enqueued. If enqueue fails, the job is detectable and recoverable via reconciliation (see §5).
- **Scheduler → Worker:** assignment is written to the store as `SCHEDULED` before being sent to the worker. A lost message is recoverable from state, not lost.
- **Worker → Scheduler/Store:** result reporting writes the execution attempt record first, then updates job state. Duplicate reports are checked against current state before being applied (idempotency).
- **Heartbeats:** simple periodic push from Worker to Worker Registry; no complex acknowledgment protocol.
- **Reconciliation sweep:** a periodic background pass re-derives "what should be true" from the store and corrects drift (stuck jobs, orphaned assignments), rather than relying on every message being delivered exactly once.

---

## 3. Job Lifecycle

```
SUBMITTED → QUEUED → SCHEDULED → RUNNING → COMPLETED
                                      ↓
                                   FAILED → RETRYING → QUEUED
QUEUED/RUNNING → CANCELLED
```

1. **Submit** — API validates the request, writes `SUBMITTED`, then enqueues → `QUEUED`.
2. **Schedule** — Scheduler dequeues the job, filters eligible/healthy workers, applies a scheduling strategy, reserves resources in the Worker Registry, writes `SCHEDULED`, and sends the assignment.
3. **Execute** — Worker acknowledges the assignment (this ack is what writes `RUNNING` — the Scheduler does not assume it optimistically), runs the job in an isolated container, captures output.
4. **Complete** — Worker reports the result; an attempt record — worker used, start time, end time, result, failure reason (if any), and attempt number — is written along with the terminal state (`COMPLETED`/`FAILED`), and reserved resources are released.
5. **Retry** — On `FAILED` with retries remaining: write `RETRYING`, then re-enqueue as `QUEUED` with an incremented attempt count.
6. **Cancel** — A `QUEUED` job is simply removed from the queue and marked `CANCELLED`. A `RUNNING` job is cancelled by sending the worker a graceful stop signal with a timeout; if the job hasn't stopped by then, the worker force-kills it. The job is marked `CANCELLED` once the worker confirms it has stopped.

---

## 4. Failure Scenarios

- **Worker dies mid-job** — Health Monitor detects missed heartbeats, marks the worker `DOWN`, and its `RUNNING` jobs are requeued/retried.
- **Worker reports late after being marked dead** — rejected using a per-worker fencing/epoch number, so a stale report from a worker that was already reassigned is ignored.
- **Scheduler crashes after reserving but before notifying the worker** — on restart, or via the periodic reconciliation sweep, jobs left in `SCHEDULED` with no `RUNNING` confirmation within a timeout are re-driven.
- **Duplicate result report** (e.g., network retry) — result handling checks that the job isn't already terminal before applying the update.
- **Lost queue message** — recoverable because the Job Store still shows the job as `QUEUED`; the reconciliation sweep re-enqueues it.

---

## 5. Key Design Decisions

| Decision | V1 Choice | Reason |
|---|---|---|
| Write vs. enqueue order | Write to Job Store first, then enqueue | A failed enqueue leaves a recoverable record instead of an orphaned queue entry |
| Marking a job `RUNNING` | Worker's acknowledgment triggers the transition, not the Scheduler's send | Keeps "stuck in `SCHEDULED`" an unambiguous signal for reconciliation |
| Detecting stale worker reports | Per-worker fencing/epoch number | Cheapest reliable way to reject duplicate/late reports from a worker already reassigned |
| Reconciliation sweep interval | ~30–45 seconds, tied to heartbeat timeout, configurable | Keeps sweep-based and heartbeat-based failure detection roughly in sync; tunable for later experiments |
| Heartbeat timeout | ~10–15 seconds (3 missed intervals) | Balances fast failure detection against false positives from transient slowness |
| Health Monitor placement | Inside the Scheduler process, not a separate service | No independent benefit yet — when the Scheduler is down, scheduling is already paused. Revisit once multiple Scheduler instances exist. |
| Scheduler topology | Single instance in V1 | Avoids leader-election complexity; acceptable since it's a bottleneck/SPOF only for scheduling, not for data durability. **V1 explicitly accepts a scheduling pause (no data loss) if the Scheduler is down; this is revisited when multi-scheduler support is added.** |

---

## 6. Explicitly Deferred (Not V1)

- Multiple scheduler instances / leader election
- Preemption and advanced fairness/starvation handling
- General-purpose workflow/DAG orchestration
- Separate streaming/event-bus infrastructure
- Multi-tenant authentication/authorization

These may be introduced later, once the core scheduling/execution/failure-handling loop is proven and measurable.
