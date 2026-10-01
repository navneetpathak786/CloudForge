package com.cloudforge.scheduler.dto;

import com.cloudforge.common.model.WorkerStatus;

import java.time.Instant;

public class WorkerResponse {

    private final String id;
    private final double cpuCapacity;
    private final long memoryCapacity;
    private final double cpuAvailable;
    private final long memoryAvailable;
    private final WorkerStatus status;
    private final Instant registeredAt;
    private final Instant lastHeartbeat;

    public WorkerResponse(String id, double cpuCapacity, long memoryCapacity,
                           double cpuAvailable, long memoryAvailable,
                           WorkerStatus status, Instant registeredAt, Instant lastHeartbeat) {
        this.id = id;
        this.cpuCapacity = cpuCapacity;
        this.memoryCapacity = memoryCapacity;
        this.cpuAvailable = cpuAvailable;
        this.memoryAvailable = memoryAvailable;
        this.status = status;
        this.registeredAt = registeredAt;
        this.lastHeartbeat = lastHeartbeat;
    }

    public String getId() {
        return id;
    }

    public double getCpuCapacity() {
        return cpuCapacity;
    }

    public long getMemoryCapacity() {
        return memoryCapacity;
    }

    public double getCpuAvailable() {
        return cpuAvailable;
    }

    public long getMemoryAvailable() {
        return memoryAvailable;
    }

    public WorkerStatus getStatus() {
        return status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }
}
