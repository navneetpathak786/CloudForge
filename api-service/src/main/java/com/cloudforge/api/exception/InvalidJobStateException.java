package com.cloudforge.api.exception;

import com.cloudforge.common.model.JobStatus;

import java.util.UUID;

public class InvalidJobStateException extends RuntimeException {

    public InvalidJobStateException(UUID id, JobStatus currentStatus) {
        super("Job " + id + " cannot be cancelled from status " + currentStatus);
    }
}
