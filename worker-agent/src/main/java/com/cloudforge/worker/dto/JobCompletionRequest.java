package com.cloudforge.worker.dto;

public class JobCompletionRequest {

    private Integer exitCode;

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }
}
