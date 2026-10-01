package com.cloudforge.api.exception;

import com.cloudforge.common.model.JobStatus;

import java.util.UUID;

public class InvalidJobStateException extends RuntimeException {

    public InvalidJobStateException(UUID id, JobStatus currentStatus) {
        this(id, currentStatus, "cancelled");
    }

    public InvalidJobStateException(UUID id, JobStatus currentStatus, String action) {
        super("Job " + id + " cannot be " + action + " from status " + currentStatus);
    }
}
