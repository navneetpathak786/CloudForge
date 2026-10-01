package com.cloudforge.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Job submission request per docs/requirements.md (section 3): name, command,
 * CPU requirement, memory requirement, max retry count, and optional priority.
 */
public class JobSubmissionRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String command;

    @NotNull
    @Positive
    private Double cpuRequirement;

    @NotNull
    @Positive
    private Long memoryRequirement;

    @NotNull
    @PositiveOrZero
    private Integer maxRetries;

    @PositiveOrZero
    private Integer priority;

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

    public Double getCpuRequirement() {
        return cpuRequirement;
    }

    public void setCpuRequirement(Double cpuRequirement) {
        this.cpuRequirement = cpuRequirement;
    }

    public Long getMemoryRequirement() {
        return memoryRequirement;
    }

    public void setMemoryRequirement(Long memoryRequirement) {
        this.memoryRequirement = memoryRequirement;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
