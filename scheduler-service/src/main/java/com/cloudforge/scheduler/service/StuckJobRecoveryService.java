package com.cloudforge.scheduler.service;

import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.queue.JobQueue;
import com.cloudforge.scheduler.repository.JobRepository;
import com.cloudforge.scheduler.repository.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recovers a job stuck RUNNING on a worker that WorkerHealthLoopRunner has
 * marked DOWN: puts the job back on the normal QUEUED path (same as any other
 * QUEUED job - the existing scheduler loop reschedules it) and releases the
 * dead worker's reservation via the same resource-release logic used for
 * finished jobs. If it fails again after actually running, api-service's
 * existing attemptCount/maxRetries logic in completeJob handles that exactly
 * as for any other failure - no retry logic is duplicated here.
 */
@Service
public class StuckJobRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(StuckJobRecoveryService.class);

    private final JobRepository jobRepository;
    private final WorkerRepository workerRepository;
    private final JobQueue jobQueue;

    public StuckJobRecoveryService(JobRepository jobRepository, WorkerRepository workerRepository,
            JobQueue jobQueue) {
        this.jobRepository = jobRepository;
        this.workerRepository = workerRepository;
        this.jobQueue = jobQueue;
    }

    @Transactional
    public void recover(JobEntity job) {
        int recovered = jobRepository.recoverStuckJob(job.getId());
        if (recovered == 0) {
            // Already recovered by an earlier pass (or it moved on in the meantime); nothing to do.
            return;
        }

        workerRepository.releaseResources(
                job.getAssignedWorkerId(), job.getCpuRequirement(), job.getMemoryRequirement());

        try {
            jobQueue.requeue(job.getId().toString());
        } catch (Exception e) {
            // The job is already durably QUEUED in the Job Store, so it is not lost - the
            // existing queue reconciliation sweep will notice it's missing from Redis and
            // requeue it, exactly as it does for a failed submission enqueue.
            log.warn("Failed to requeue recovered job {} onto the Redis queue", job.getId(), e);
        }
    }
}
