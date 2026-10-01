# CloudForge — Requirements

## 1. Users

CloudForge has two primary actors:

### User

A user can:

* Submit a job
* View job status
* View job details
* View job execution history
* Cancel a queued or running job

### Worker

A worker node:

* Registers with CloudForge
* Reports its available resources
* Sends periodic heartbeats
* Receives assigned jobs
* Executes jobs
* Reports job results
* Can become unavailable or recover from failure

---

## 2. Job Lifecycle

Every job must have a clearly defined lifecycle.

Initial states:

```text
SUBMITTED
    ↓
QUEUED
    ↓
SCHEDULED
    ↓
RUNNING
    ↓
COMPLETED
```

Failure paths:

```text
RUNNING → FAILED
RUNNING → RETRYING → QUEUED
QUEUED → CANCELLED
RUNNING → CANCELLED
```

The system must maintain the current state of every job.

---

## 3. Job Submission

A user must be able to submit a job containing at least:

* Job name
* Execution command
* CPU requirement
* Memory requirement
* Maximum retry count
* Optional priority

The system must validate the request before accepting the job.

After successful submission, the job receives a unique identifier.

---

## 4. Job Scheduling

The scheduler is responsible for assigning queued jobs to suitable workers.

A worker can only receive a job if it has sufficient available resources.

The scheduler must:

* Identify eligible workers
* Consider worker health
* Consider available resources
* Select a worker using a scheduling strategy
* Reserve the required resources
* Assign the job
* Record the scheduling decision

The scheduling system should support multiple strategies so they can be compared experimentally.

---

## 5. Worker Management

Workers must be dynamically registered with CloudForge.

For each worker, the system should track:

* Unique worker ID
* CPU capacity
* Memory capacity
* Current resource usage
* Worker status
* Last heartbeat
* Currently running jobs

Workers must periodically send heartbeats.

If a worker stops sending heartbeats for a defined period, CloudForge must detect that the worker is unavailable.

---

## 6. Job Execution

Jobs must execute in an isolated environment.

The worker must:

1. Receive a job assignment.
2. Allocate the required resources.
3. Start the job.
4. Capture execution output.
5. Track execution status.
6. Report success or failure.
7. Release allocated resources.

---

## 7. Failure Handling

CloudForge must handle failures such as:

* Worker becoming unavailable
* Job execution failure
* Duplicate job events
* Scheduler restart
* Temporary communication failures

The system should prevent a failed worker from receiving new jobs.

Failed jobs may be retried according to their configured retry policy.

Job processing should be designed to be idempotent where duplicate events are possible.

---

## 8. Job History

CloudForge must retain sufficient information about job execution to understand what happened.

For each execution attempt, the system should be able to record:

* Worker used
* Start time
* End time
* Result
* Failure reason, if applicable
* Retry/attempt number

This information will later be used for debugging and performance analysis.

---

## 9. Observability

The system must expose useful operational information, including:

* Jobs submitted
* Jobs completed
* Jobs failed
* Jobs currently running
* Job execution duration
* Job queue/wait time
* Worker utilization
* Scheduler decision latency
* Worker health

The system should provide dashboards for observing the cluster and job activity.

---

## 10. Performance Evaluation

CloudForge should support experiments comparing different scheduling strategies.

Experiments should measure metrics such as:

* Average job wait time
* Job throughput
* Worker resource utilization
* Scheduling latency
* Job completion time
* Failure recovery time

The results should be reproducible and documented.

---

## 11. Non-Functional Requirements

CloudForge should prioritize:

### Reliability

The system should recover gracefully from common component and worker failures.

### Scalability

The architecture should allow additional workers to be added without changing the scheduler's fundamental design.

### Maintainability

Services and components should have clear responsibilities.

### Testability

Important scheduling and failure-handling behavior must be testable independently.

### Observability

Important system behavior should be measurable rather than hidden.

### Simplicity

Complexity should only be introduced when it solves an actual problem.

---

## 12. Initial Success Criteria

The first meaningful version of CloudForge is successful when:

1. A user can submit a job.
2. The job enters a queue.
3. A healthy worker is selected automatically.
4. The worker executes the job in isolation.
5. The result is reported back.
6. The job reaches a final state.
7. Worker failure can be detected.
8. Failed jobs can be retried or rescheduled.
9. The system provides enough metrics to understand its behavior.

Everything beyond this is an extension of the core system.
