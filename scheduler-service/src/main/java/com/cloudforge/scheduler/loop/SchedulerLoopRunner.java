package com.cloudforge.scheduler.loop;

import com.cloudforge.scheduler.queue.JobQueue;
import com.cloudforge.scheduler.service.SchedulingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically drains the current Redis backlog of QUEUED jobs. Bounded to the
 * queue's size at the start of each tick, so a job that gets put back (no eligible
 * worker yet) isn't immediately re-picked within the same pass.
 */
@Component
@ConditionalOnProperty(name = "scheduler.loop.enabled", matchIfMissing = true)
public class SchedulerLoopRunner {

    private final SchedulingService schedulingService;
    private final JobQueue jobQueue;

    public SchedulerLoopRunner(SchedulingService schedulingService, JobQueue jobQueue) {
        this.schedulingService = schedulingService;
        this.jobQueue = jobQueue;
    }

    @Scheduled(fixedDelayString = "${scheduler.loop.fixed-delay-ms:1000}")
    public void run() {
        long batch = jobQueue.size();
        for (long i = 0; i < batch; i++) {
            schedulingService.scheduleNext();
        }
    }
}
