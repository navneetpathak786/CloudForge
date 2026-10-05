package com.cloudforge.scheduler.service;

import com.cloudforge.common.model.JobStatus;
import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.queue.JobQueue;
import com.cloudforge.scheduler.repository.JobRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Postgres is the source of truth for QUEUED jobs; Redis is a disposable work
 * queue that can lose an entry (e.g. an enqueue that failed silently) without
 * the job's QUEUED status being lost. This re-enqueues any QUEUED job that's
 * currently missing from the Redis queue, so it isn't stuck forever.
 */
@Service
public class JobQueueReconciliationService {

    private static final int MAX_JOBS_PER_RUN = 100;

    private final JobRepository jobRepository;
    private final JobQueue jobQueue;

    public JobQueueReconciliationService(JobRepository jobRepository, JobQueue jobQueue) {
        this.jobRepository = jobRepository;
        this.jobQueue = jobQueue;
    }

    public void reconcile() {
        List<JobEntity> queuedJobs = jobRepository.findByStatus(JobStatus.QUEUED, Limit.of(MAX_JOBS_PER_RUN));
        for (JobEntity job : queuedJobs) {
            String jobId = job.getId().toString();
            if (!jobQueue.isQueued(jobId)) {
                jobQueue.requeue(jobId);
            }
        }
    }
}
