package com.cloudforge.scheduler.entity;

import com.cloudforge.common.model.WorkerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "workers")
public class WorkerEntity {

    @Id
    private String id;

    @Column(name = "cpu_capacity", nullable = false)
    private double cpuCapacity;

    @Column(name = "memory_capacity", nullable = false)
    private long memoryCapacity;

    @Column(name = "cpu_available", nullable = false)
    private double cpuAvailable;

    @Column(name = "memory_available", nullable = false)
    private long memoryAvailable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkerStatus status;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "last_heartbeat")
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

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }
}
