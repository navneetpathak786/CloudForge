package com.cloudforge.common.model;

import java.time.Instant;

/**
 * Worker data shape per docs/requirements.md (section 5). Plain data holder,
 * no behavior — registration/heartbeat logic is not implemented yet.
 */
public class Worker {

    private String id;
    private double cpuCapacity;
    private long memoryCapacity;
    private double cpuAvailable;
    private long memoryAvailable;
    private WorkerStatus status;
    private Instant lastHeartbeat;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getCpuCapacity() {
        return cpuCapacity;
    }

    public void setCpuCapacity(double cpuCapacity) {
        this.cpuCapacity = cpuCapacity;
    }

    public long getMemoryCapacity() {
        return memoryCapacity;
    }

    public void setMemoryCapacity(long memoryCapacity) {
        this.memoryCapacity = memoryCapacity;
    }

    public double getCpuAvailable() {
        return cpuAvailable;
    }

    public void setCpuAvailable(double cpuAvailable) {
        this.cpuAvailable = cpuAvailable;
    }

    public long getMemoryAvailable() {
        return memoryAvailable;
    }

    public void setMemoryAvailable(long memoryAvailable) {
        this.memoryAvailable = memoryAvailable;
    }

    public WorkerStatus getStatus() {
        return status;
    }

    public void setStatus(WorkerStatus status) {
        this.status = status;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }
}
