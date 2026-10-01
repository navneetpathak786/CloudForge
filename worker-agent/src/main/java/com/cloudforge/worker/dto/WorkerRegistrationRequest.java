package com.cloudforge.worker.dto;

public class WorkerRegistrationRequest {

    private Double cpuCapacity;
    private Long memoryCapacity;

    public Double getCpuCapacity() {
        return cpuCapacity;
    }

    public void setCpuCapacity(Double cpuCapacity) {
        this.cpuCapacity = cpuCapacity;
    }

    public Long getMemoryCapacity() {
        return memoryCapacity;
    }

    public void setMemoryCapacity(Long memoryCapacity) {
        this.memoryCapacity = memoryCapacity;
    }
}
