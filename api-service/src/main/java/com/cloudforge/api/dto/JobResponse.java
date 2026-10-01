package com.cloudforge.api.dto;

import com.cloudforge.common.model.JobStatus;

import java.time.Instant;

public class JobResponse {

    private final String id;
    private final String name;
    private final String command;
    private final double cpuRequirement;
    private final long memoryRequirement;
    private final int maxRetries;
    private final Integer priority;
    private final JobStatus status;
    private final Instant createdAt;

    public JobResponse(String id, String name, String command, double cpuRequirement,
                        long memoryRequirement, int maxRetries, Integer priority,
                        JobStatus status, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.command = command;
        this.cpuRequirement = cpuRequirement;
        this.memoryRequirement = memoryRequirement;
        this.maxRetries = maxRetries;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCommand() {
        return command;
    }

    public double getCpuRequirement() {
        return cpuRequirement;
    }

    public long getMemoryRequirement() {
        return memoryRequirement;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public Integer getPriority() {
        return priority;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
