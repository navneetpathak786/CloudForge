package com.cloudforge.scheduler.service;

import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.queue.JobQueue;
import com.cloudforge.scheduler.repository.JobRepository;
import com.cloudforge.scheduler.repository.WorkerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StuckJobRecoveryServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private JobQueue jobQueue;

    private JobEntity stuckJob() {
        JobEntity job = new JobEntity();
        job.setId(UUID.randomUUID());
        job.setCpuRequirement(1.0);
        job.setMemoryRequirement(512L);
        job.setAssignedWorkerId("worker-1");
        return job;
    }

    @Test
    void recover_whenStillRunning_releasesWorkerResourcesAndRequeuesJob() {
        StuckJobRecoveryService service = new StuckJobRecoveryService(jobRepository, workerRepository, jobQueue);
        JobEntity job = stuckJob();
        when(jobRepository.recoverStuckJob(job.getId())).thenReturn(1);

        service.recover(job);

        verify(workerRepository).releaseResources("worker-1", 1.0, 512L);
        verify(jobQueue).requeue(job.getId().toString());
    }

    @Test
    void recover_whenAlreadyRecovered_doesNotReleaseOrRequeueAgain() {
        StuckJobRecoveryService service = new StuckJobRecoveryService(jobRepository, workerRepository, jobQueue);
        JobEntity job = stuckJob();
        when(jobRepository.recoverStuckJob(job.getId())).thenReturn(0);

        service.recover(job);

        verify(workerRepository, never()).releaseResources(any(), anyDouble(), anyLong());
        verify(jobQueue, never()).requeue(any());
    }

    @Test
    void recover_whenRequeueFails_stillSucceedsSinceReconciliationWillPickItUp() {
        StuckJobRecoveryService service = new StuckJobRecoveryService(jobRepository, workerRepository, jobQueue);
        JobEntity job = stuckJob();
        when(jobRepository.recoverStuckJob(job.getId())).thenReturn(1);
        doThrow(new RuntimeException("redis unavailable")).when(jobQueue).requeue(any());

        assertThatCode(() -> service.recover(job)).doesNotThrowAnyException();

        verify(workerRepository).releaseResources("worker-1", 1.0, 512L);
    }
}
