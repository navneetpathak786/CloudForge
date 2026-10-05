package com.cloudforge.scheduler.entity;

import com.cloudforge.common.model.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps onto the same "jobs" table api-service owns/migrates. assigned_worker_id
 * is the one column only the scheduler writes.
 */
@Entity
@Table(name = "jobs")
public class JobEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String command;

    @Column(name = "cpu_requirement", nullable = false)
    private double cpuRequirement;

    @Column(name = "memory_requirement", nullable = false)
    private long memoryRequirement;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries;

    @Column
    private Integer priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "assigned_worker_id")
    private String assignedWorkerId;

    @Column(name = "resources_released_at")
    private Instant resourcesReleasedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public String getAssignedWorkerId() {
        return assignedWorkerId;
    }

    public void setAssignedWorkerId(String assignedWorkerId) {
        this.assignedWorkerId = assignedWorkerId;
    }

    public Instant getResourcesReleasedAt() {
        return resourcesReleasedAt;
    }

    public void setResourcesReleasedAt(Instant resourcesReleasedAt) {
        this.resourcesReleasedAt = resourcesReleasedAt;
    }
}
