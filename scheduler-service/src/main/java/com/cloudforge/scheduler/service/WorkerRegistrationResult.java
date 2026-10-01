package com.cloudforge.scheduler.service;

import com.cloudforge.scheduler.dto.WorkerResponse;

public record WorkerRegistrationResult(WorkerResponse response, boolean alreadyRegistered) {
}
