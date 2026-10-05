package com.cloudforge.scheduler.loop;

import com.cloudforge.scheduler.service.JobQueueReconciliationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically re-enqueues QUEUED jobs that are missing from the Redis queue,
 * so a lost enqueue doesn't leave a job stuck forever without a scheduler restart.
 */
@Component
public class JobQueueReconciliationLoopRunner {

    private final JobQueueReconciliationService reconciliationService;

    public JobQueueReconciliationLoopRunner(JobQueueReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(fixedDelayString = "${scheduler.reconciliation.fixed-delay-ms:5000}")
    public void run() {
        reconciliationService.reconcile();
    }
}
