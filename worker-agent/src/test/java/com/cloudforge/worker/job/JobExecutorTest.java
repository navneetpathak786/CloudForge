package com.cloudforge.worker.job;

import com.cloudforge.worker.client.JobApiClient;
import com.cloudforge.worker.dto.JobResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobExecutorTest {

    @Mock
    private JobApiClient jobApiClient;

    private JobResponse job(String command) {
        JobResponse job = new JobResponse();
        job.setId("job-1");
        job.setName("test-job");
        job.setCommand(command);
        return job;
    }

    @Test
    void execute_withSucceedingCommand_startsThenCompletesWithZeroExitCode() {
        JobExecutor executor = new JobExecutor(jobApiClient);

        executor.execute(job("exit 0"));

        verify(jobApiClient).startJob("job-1");
        verify(jobApiClient).completeJob("job-1", 0);
    }

    @Test
    void execute_withFailingCommand_completesWithTheProcessExitCode() {
        JobExecutor executor = new JobExecutor(jobApiClient);

        executor.execute(job("exit 7"));

        verify(jobApiClient).startJob("job-1");
        verify(jobApiClient).completeJob("job-1", 7);
    }
}
