package com.cloudforge.common.model;

import java.time.Instant;

/**
 * Job data shape per docs/requirements.md (section 3). Plain data holder,
 * no behavior — scheduling/lifecycle logic is not implemented yet.
 */
public class Job {

    private String id;
    private String name;
    private String command;
    private double cpuRequirement;
    private long memoryRequirement;
    private int maxRetries;
    private Integer priority;
    private JobStatus status;
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public double getCpuRequirement() {
        return cpuRequirement;
    }

    public void setCpuRequirement(double cpuRequirement) {
        this.cpuRequirement = cpuRequirement;
    }

    public long getMemoryRequirement() {
        return memoryRequirement;
    }

    public void setMemoryRequirement(long memoryRequirement) {
        this.memoryRequirement = memoryRequirement;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
