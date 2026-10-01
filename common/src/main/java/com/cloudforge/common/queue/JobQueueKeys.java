package com.cloudforge.common.queue;

/**
 * Redis key names shared between api-service (producer) and scheduler-service
 * (consumer) so both sides agree on where QUEUED job ids live.
 */
public final class JobQueueKeys {

    public static final String QUEUED_JOBS_KEY = "cloudforge:jobs:queued";

    private JobQueueKeys() {
    }
}
