package com.cloudforge.api.service;

import com.cloudforge.api.dto.JobResponse;
import com.cloudforge.api.dto.JobSubmissionRequest;
import com.cloudforge.api.entity.JobEntity;
import com.cloudforge.api.exception.InvalidJobStateException;
import com.cloudforge.api.exception.JobNotFoundException;
import com.cloudforge.api.queue.JobQueue;
import com.cloudforge.api.repository.JobRepository;
import com.cloudforge.common.model.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles job submission: validation is enforced on JobSubmissionRequest via
 * Bean Validation before this is reached. A validated job is written to the
 * Job Store and enqueued in the same step (SUBMITTED has no separate durable
 * state in V1 - nothing yet reviews a job between validation and queueing).
 */
@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    /**
     * SUBMITTED is kept in this set defensively even though no job is currently
     * observed in that state (see submitJob) - it remains a valid lifecycle state
     * per docs/requirements.md and should stay cancellable if that ever changes.
     */
    private static final Set<JobStatus> CANCELLABLE_STATUSES =
            EnumSet.of(JobStatus.SUBMITTED, JobStatus.QUEUED, JobStatus.RUNNING);

    private final JobRepository jobRepository;
    private final JobQueue jobQueue;

    public JobService(JobRepository jobRepository, JobQueue jobQueue) {
        this.jobRepository = jobRepository;
        this.jobQueue = jobQueue;
    }

    @Transactional
    public JobResponse submitJob(JobSubmissionRequest request) {
        JobEntity entity = new JobEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(request.getName());
        entity.setCommand(request.getCommand());
        entity.setCpuRequirement(request.getCpuRequirement());
        entity.setMemoryRequirement(request.getMemoryRequirement());
        entity.setMaxRetries(request.getMaxRetries());
        entity.setAttemptCount(0);
        entity.setPriority(request.getPriority());
        entity.setStatus(JobStatus.QUEUED);
        entity.setCreatedAt(Instant.now());

        JobEntity saved = jobRepository.save(entity);

        try {
            jobQueue.enqueue(saved.getId().toString());
        } catch (Exception e) {
            // The job is already durably QUEUED in the Job Store, so it is not lost -
            // it just won't be picked up until a reconciliation pass (not yet implemented)
            // notices it's QUEUED but missing from the Redis queue.
            log.warn("Failed to enqueue job {} onto the Redis queue", saved.getId(), e);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID id) {
        return jobRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    /**
     * Worker's acknowledgment of a SCHEDULED job assignment writes RUNNING, per
     * docs/architecture.md step 3 - the Scheduler does not assume this optimistically.
     */
    @Transactional
    public JobResponse startJob(UUID id) {
        JobEntity entity = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));

        if (entity.getStatus() != JobStatus.SCHEDULED) {
            throw new InvalidJobStateException(id, entity.getStatus(), "started");
        }

        entity.setStatus(JobStatus.RUNNING);
        JobEntity saved = jobRepository.save(entity);
        return toResponse(saved);
    }

    /**
     * Worker reports the process exit code once the job finishes, per
     * docs/architecture.md step 4 - COMPLETED on exit code 0. On failure, this
     * also decides retry eligibility (step 5): if attemptCount is still within
     * maxRetries after incrementing, the job goes back to QUEUED and is
     * re-enqueued the same best-effort way submitJob does - a failed enqueue
     * leaves it durably QUEUED for the reconciliation sweep to pick up.
     * Requiring the job to still be RUNNING also guards against a duplicate/late
     * completion report re-triggering a retry once this has already run once.
     */
    @Transactional
    public JobResponse completeJob(UUID id, int exitCode) {
        JobEntity entity = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));

        if (entity.getStatus() != JobStatus.RUNNING) {
            throw new InvalidJobStateException(id, entity.getStatus(), "completed");
        }

        if (exitCode == 0) {
            entity.setStatus(JobStatus.COMPLETED);
            JobEntity saved = jobRepository.save(entity);
            return toResponse(saved);
        }

        entity.setAttemptCount(entity.getAttemptCount() + 1);
        if (entity.getAttemptCount() <= entity.getMaxRetries()) {
            entity.setStatus(JobStatus.QUEUED);
            JobEntity saved = jobRepository.save(entity);

            try {
                jobQueue.enqueue(saved.getId().toString());
            } catch (Exception e) {
                log.warn("Failed to re-enqueue job {} onto the Redis queue for retry", saved.getId(), e);
            }

            return toResponse(saved);
        }

        entity.setStatus(JobStatus.FAILED);
        JobEntity saved = jobRepository.save(entity);
        return toResponse(saved);
    }

    /**
     * Marks a QUEUED/RUNNING (or still-SUBMITTED) job CANCELLED. For a RUNNING job the
     * architecture calls for a graceful stop signal to the worker first; that requires
     * scheduler/worker-agent coordination that doesn't exist yet, so this only updates
     * the store for now - worker-side stop signaling is deferred with that work.
     */
    @Transactional
    public JobResponse cancelJob(UUID id) {
        JobEntity entity = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));

        if (!CANCELLABLE_STATUSES.contains(entity.getStatus())) {
            throw new InvalidJobStateException(id, entity.getStatus());
        }

        entity.setStatus(JobStatus.CANCELLED);
        JobEntity saved = jobRepository.save(entity);
        return toResponse(saved);
    }

    private JobResponse toResponse(JobEntity entity) {
        return new JobResponse(
                entity.getId().toString(),
                entity.getName(),
                entity.getCommand(),
                entity.getCpuRequirement(),
                entity.getMemoryRequirement(),
                entity.getMaxRetries(),
                entity.getAttemptCount(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getCreatedAt());
    }
}
