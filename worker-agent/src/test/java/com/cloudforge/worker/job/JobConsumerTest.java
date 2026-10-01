package com.cloudforge.worker.job;

import com.cloudforge.worker.client.JobApiClient;
import com.cloudforge.worker.dto.JobResponse;
import com.cloudforge.worker.queue.WorkerJobQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobConsumerTest {

    @Mock
    private WorkerJobQueue workerJobQueue;

    @Mock
    private JobApiClient jobApiClient;

    @Mock
    private JobExecutor jobExecutor;

    @Test
    void pollForJob_withQueuedJobId_fetchesJobDetailsAndExecutesIt() {
        JobConsumer consumer = new JobConsumer(workerJobQueue, jobApiClient, jobExecutor);
        JobResponse job = new JobResponse();
        job.setId("job-1");
        job.setName("test-job");
        when(workerJobQueue.dequeue()).thenReturn(Optional.of("job-1"));
        when(jobApiClient.getJob("job-1")).thenReturn(job);

        consumer.pollForJob();

        verify(jobApiClient).getJob("job-1");
        verify(jobExecutor).execute(job);
    }

    @Test
    void pollForJob_withEmptyQueue_doesNotFetchOrExecute() {
        JobConsumer consumer = new JobConsumer(workerJobQueue, jobApiClient, jobExecutor);
        when(workerJobQueue.dequeue()).thenReturn(Optional.empty());

        consumer.pollForJob();

        verify(jobApiClient, never()).getJob(any());
        verify(jobExecutor, never()).execute(any());
    }

    @Test
    void pollForJob_whenJobFetchFails_doesNotPropagateException() {
        JobConsumer consumer = new JobConsumer(workerJobQueue, jobApiClient, jobExecutor);
        when(workerJobQueue.dequeue()).thenReturn(Optional.of("job-1"));
        when(jobApiClient.getJob("job-1")).thenThrow(new RuntimeException("api unavailable"));

        consumer.pollForJob();

        verify(jobExecutor, never()).execute(any());
    }

    @Test
    void pollForJob_whenExecutionFails_doesNotPropagateException() {
        JobConsumer consumer = new JobConsumer(workerJobQueue, jobApiClient, jobExecutor);
        JobResponse job = new JobResponse();
        job.setId("job-1");
        when(workerJobQueue.dequeue()).thenReturn(Optional.of("job-1"));
        when(jobApiClient.getJob("job-1")).thenReturn(job);
        doThrow(new RuntimeException("execution failed")).when(jobExecutor).execute(job);

        consumer.pollForJob();
    }
}
