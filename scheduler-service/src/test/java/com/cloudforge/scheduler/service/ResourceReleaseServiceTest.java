package com.cloudforge.scheduler.service;

import com.cloudforge.common.model.JobStatus;
import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.repository.JobRepository;
import com.cloudforge.scheduler.repository.WorkerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceReleaseServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private WorkerRepository workerRepository;

    private JobEntity job(JobStatus status) {
        JobEntity job = new JobEntity();
        job.setId(UUID.randomUUID());
        job.setStatus(status);
        job.setCpuRequirement(1.0);
        job.setMemoryRequirement(512L);
        job.setAssignedWorkerId("worker-1");
        return job;
    }

    @Test
    void release_whenNotYetReleased_marksReleasedAndReturnsResourcesToWorker() {
        ResourceReleaseService service = new ResourceReleaseService(jobRepository, workerRepository);
        JobEntity job = job(JobStatus.COMPLETED);
        when(jobRepository.markResourcesReleased(job.getId())).thenReturn(1);

        service.release(job);

        verify(workerRepository).releaseResources("worker-1", 1.0, 512L);
    }

    @Test
    void release_whenAlreadyReleased_doesNotReleaseResourcesAgain() {
        ResourceReleaseService service = new ResourceReleaseService(jobRepository, workerRepository);
        JobEntity job = job(JobStatus.FAILED);
        when(jobRepository.markResourcesReleased(job.getId())).thenReturn(0);

        service.release(job);

        verify(workerRepository, never()).releaseResources(any(), anyDouble(), anyLong());
    }
}
