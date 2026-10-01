package com.cloudforge.worker.job;

import com.cloudforge.worker.client.JobApiClient;
import com.cloudforge.worker.dto.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Acknowledges a fetched job as RUNNING, runs its command as a local OS
 * process via ProcessBuilder, and reports the exit code so api-service can
 * mark it COMPLETED (exit code 0) or FAILED (otherwise).
 */
@Component
public class JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(JobExecutor.class);

    private final JobApiClient jobApiClient;

    public JobExecutor(JobApiClient jobApiClient) {
        this.jobApiClient = jobApiClient;
    }

    public void execute(JobResponse job) {
        jobApiClient.startJob(job.getId());

        int exitCode;
        try {
            exitCode = runCommand(job.getCommand());
        } catch (Exception e) {
            log.warn("Failed to run command for job {}", job.getId(), e);
            exitCode = 1;
        }

        jobApiClient.completeJob(job.getId(), exitCode);
    }

    private int runCommand(String command) throws Exception {
        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        List<String> commandParts = isWindows
                ? List.of("cmd.exe", "/c", command)
                : List.of("sh", "-c", command);
        Process process = new ProcessBuilder(commandParts).inheritIO().start();
        return process.waitFor();
    }
}
