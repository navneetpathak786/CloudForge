package com.cloudforge.scheduler.service;

import com.cloudforge.common.model.JobStatus;
import com.cloudforge.common.model.WorkerStatus;
import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.entity.WorkerEntity;
import com.cloudforge.scheduler.queue.JobQueue;
import com.cloudforge.scheduler.repository.JobRepository;
import com.cloudforge.scheduler.repository.WorkerRepository;
import com.cloudforge.scheduler.scheduling.SchedulingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Drives QUEUED -> SCHEDULED: dequeues one job id, filters eligible (healthy,
 * sufficient-capacity) workers, applies the pluggable SchedulingStrategy, and
 * atomically reserves resources + marks the job SCHEDULED. A job that can't be
 * placed this pass is put back on the queue rather than dropped.
 */
@Service
public class SchedulingService {

    private static final Logger log = LoggerFactory.getLogger(SchedulingService.class);

    private final JobQueue jobQueue;
    private final JobRepository jobRepository;
    private final WorkerRepository workerRepository;
    private final SchedulingStrategy schedulingStrategy;

    public SchedulingService(JobQueue jobQueue,
                              JobRepository jobRepository,
                              WorkerRepository workerRepository,
                              SchedulingStrategy schedulingStrategy) {
        this.jobQueue = jobQueue;
        this.jobRepository = jobRepository;
        this.workerRepository = workerRepository;
        this.schedulingStrategy = schedulingStrategy;
    }

    /**
     * Attempts to place the next queued job, if any. Returns true if a job id was
     * dequeued (whether or not it was placed), so callers can bound a drain loop by
     * the queue's starting size instead of looping forever.
     */
    @Transactional
    public boolean scheduleNext() {
        Optional<String> jobIdOpt = jobQueue.dequeue();
        if (jobIdOpt.isEmpty()) {
            return false;
        }

        String jobIdRaw = jobIdOpt.get();
        UUID jobId = UUID.fromString(jobIdRaw);

        JobEntity job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != JobStatus.QUEUED) {
            log.warn("Dequeued job {} is not schedulable (missing or no longer QUEUED); dropping from this pass", jobIdRaw);
            return true;
        }

        List<WorkerEntity> eligible = workerRepository.findByStatus(WorkerStatus.HEALTHY).stream()
                .filter(w -> w.getCpuAvailable() >= job.getCpuRequirement()
                        && w.getMemoryAvailable() >= job.getMemoryRequirement())
                .toList();

        Optional<WorkerEntity> chosen = schedulingStrategy.selectWorker(job, eligible);
        if (chosen.isEmpty()) {
            jobQueue.requeue(jobIdRaw);
            return true;
        }

        int updated = workerRepository.reserveResources(
                chosen.get().getId(), job.getCpuRequirement(), job.getMemoryRequirement());
        if (updated == 0) {
            // Lost the race to another reservation against the same worker; retry next pass.
            jobQueue.requeue(jobIdRaw);
            return true;
        }

        job.setStatus(JobStatus.SCHEDULED);
        job.setAssignedWorkerId(chosen.get().getId());
        jobRepository.save(job);
        return true;
    }
}
