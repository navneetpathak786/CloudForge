package com.cloudforge.common.model;

/**
 * Job lifecycle states as defined in docs/requirements.md (section 2).
 */
public enum JobStatus {
    SUBMITTED,
    QUEUED,
    SCHEDULED,
    RUNNING,
    COMPLETED,
    FAILED,
    RETRYING,
    CANCELLED
}
