package com.cloudforge.api.dto;

import jakarta.validation.constraints.NotNull;

public class JobCompletionRequest {

    @NotNull
    private Integer exitCode;

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }
}
