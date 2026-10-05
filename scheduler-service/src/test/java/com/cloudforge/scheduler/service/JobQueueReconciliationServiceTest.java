package com.cloudforge.scheduler.service;

import com.cloudforge.common.model.JobStatus;
import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.queue.JobQueue;
import com.cloudforge.scheduler.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Limit;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobQueueReconciliationServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobQueue jobQueue;

    private JobEntity job() {
        JobEntity job = new JobEntity();
        job.setId(UUID.randomUUID());
        job.setStatus(JobStatus.QUEUED);
        return job;
    }

    @Test
    void reconcile_withQueuedJobMissingFromRedis_requeuesIt() {
        JobQueueReconciliationService service = new JobQueueReconciliationService(jobRepository, jobQueue);
        JobEntity job = job();
        when(jobRepository.findByStatus(JobStatus.QUEUED, Limit.of(100))).thenReturn(List.of(job));
        when(jobQueue.isQueued(job.getId().toString())).thenReturn(false);

        service.reconcile();

        verify(jobQueue).requeue(job.getId().toString());
    }

    @Test
    void reconcile_withQueuedJobAlreadyInRedis_doesNotRequeueIt() {
        JobQueueReconciliationService service = new JobQueueReconciliationService(jobRepository, jobQueue);
        JobEntity job = job();
        when(jobRepository.findByStatus(JobStatus.QUEUED, Limit.of(100))).thenReturn(List.of(job));
        when(jobQueue.isQueued(job.getId().toString())).thenReturn(true);

        service.reconcile();

        verify(jobQueue, never()).requeue(job.getId().toString());
    }

    @Test
    void reconcile_withNoQueuedJobs_doesNothing() {
        JobQueueReconciliationService service = new JobQueueReconciliationService(jobRepository, jobQueue);
        when(jobRepository.findByStatus(JobStatus.QUEUED, Limit.of(100))).thenReturn(List.of());

        service.reconcile();

        verify(jobQueue, never()).requeue(any());
    }
}
