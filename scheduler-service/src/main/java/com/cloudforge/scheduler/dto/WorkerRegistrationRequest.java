package com.cloudforge.scheduler.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class WorkerRegistrationRequest {

    @NotNull
    @Positive
    private Double cpuCapacity;

    @NotNull
    @Positive
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
