package com.cloudforge.scheduler.loop;

import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.queue.JobQueue;
import com.cloudforge.scheduler.repository.JobRepository;
import com.cloudforge.scheduler.service.SchedulingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Periodically drains the current Redis backlog of QUEUED jobs. Bounded to the
 * queue's size at the start of each tick, so a job that gets put back (no eligible
 * worker yet) isn't immediately re-picked within the same pass.
 *
 * Before each job reaches SchedulingService.scheduleNext() (unchanged - still
 * dequeues and processes one job at a time, same worker eligibility/capacity
 * checks and scheduling strategy), the drained batch is re-pushed onto the
 * queue ordered by priority (descending, null last) so higher-priority jobs are
 * dequeued first. The sort is stable, so equal-priority jobs keep their
 * original (arrival) order.
 */
@Component
@ConditionalOnProperty(name = "scheduler.loop.enabled", matchIfMissing = true)
public class SchedulerLoopRunner {

    private final SchedulingService schedulingService;
    private final JobQueue jobQueue;
    private final JobRepository jobRepository;

    public SchedulerLoopRunner(SchedulingService schedulingService, JobQueue jobQueue, JobRepository jobRepository) {
        this.schedulingService = schedulingService;
        this.jobQueue = jobQueue;
        this.jobRepository = jobRepository;
    }

    @Scheduled(fixedDelayString = "${scheduler.loop.fixed-delay-ms:1000}")
    public void run() {
        long batch = jobQueue.size();
        if (batch == 0) {
            return;
        }

        List<String> drained = new ArrayList<>();
        for (long i = 0; i < batch; i++) {
            jobQueue.dequeue().ifPresent(drained::add);
        }

        for (String jobId : byPriorityDescending(drained)) {
            jobQueue.requeue(jobId);
        }

        for (int i = 0; i < drained.size(); i++) {
            schedulingService.scheduleNext();
        }
    }

    List<String> byPriorityDescending(List<String> jobIds) {
        List<UUID> ids = new ArrayList<>();
        for (String jobId : jobIds) {
            ids.add(UUID.fromString(jobId));
        }

        Map<UUID, Integer> priorityById = new HashMap<>();
        for (JobEntity job : jobRepository.findAllById(ids)) {
            priorityById.put(job.getId(), job.getPriority());
        }

        List<String> ordered = new ArrayList<>(jobIds);
        ordered.sort(Comparator.comparing(
                (String jobId) -> priorityById.get(UUID.fromString(jobId)),
                Comparator.nullsFirst(Comparator.<Integer>naturalOrder())).reversed());
        return ordered;
    }
}
