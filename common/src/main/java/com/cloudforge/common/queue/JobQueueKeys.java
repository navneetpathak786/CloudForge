package com.cloudforge.common.queue;

/**
 * Redis key names shared between api-service (producer) and scheduler-service
 * (consumer) so both sides agree on where QUEUED job ids live.
 */
public final class JobQueueKeys {

    public static final String QUEUED_JOBS_KEY = "cloudforge:jobs:queued";

    /**
     * Per-worker delivery queue a job id is pushed onto once it's assigned to
     * that worker, so the worker can consume only jobs meant for it.
     */
    public static String workerJobsKey(String workerId) {
        return "cloudforge:worker:" + workerId + ":jobs";
    }

    private JobQueueKeys() {
    }
}
