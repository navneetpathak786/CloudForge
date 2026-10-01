package com.cloudforge.common.model;

/**
 * Worker health states as defined in docs/requirements.md (section 5).
 */
public enum WorkerStatus {
    REGISTERING,
    HEALTHY,
    UNHEALTHY,
    DOWN
}
