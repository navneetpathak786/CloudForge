package com.cloudforge.scheduler.service;

import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.repository.JobRepository;
import com.cloudforge.scheduler.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Releases a finished job's reserved worker capacity back to the pool. Marking
 * the job released and releasing the capacity happen in one transaction, guarded
 * by the same atomic-conditional-update pattern used for scheduling, so a job's
 * resources are released at most once even if reconciliation runs more than once.
 */
@Service
public class ResourceReleaseService {

    private final JobRepository jobRepository;
    private final WorkerRepository workerRepository;

    public ResourceReleaseService(JobRepository jobRepository, WorkerRepository workerRepository) {
        this.jobRepository = jobRepository;
        this.workerRepository = workerRepository;
    }

    @Transactional
    public void release(JobEntity job) {
        UUID jobId = job.getId();
        int marked = jobRepository.markResourcesReleased(jobId);
        if (marked == 0) {
            // Already released by an earlier pass; nothing to do.
            return;
        }

        workerRepository.releaseResources(
                job.getAssignedWorkerId(), job.getCpuRequirement(), job.getMemoryRequirement());
    }
}
