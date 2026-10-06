package com.cloudforge.scheduler.loop;

import com.cloudforge.common.model.WorkerStatus;
import com.cloudforge.scheduler.entity.WorkerEntity;
import com.cloudforge.scheduler.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Periodically detects HEALTHY workers whose heartbeat has gone stale and
 * marks them DOWN, per docs/architecture.md's Health Monitor - a worker that
 * stops heartbeating is presumed failed so its jobs can be recovered
 * (StuckJobRecoveryLoopRunner) rather than left waiting on a worker that may
 * never come back.
 */
@Component
@ConditionalOnProperty(name = "scheduler.worker-health.enabled", matchIfMissing = true)
public class WorkerHealthLoopRunner {

    private final WorkerRepository workerRepository;
    private final long heartbeatTimeoutMs;

    public WorkerHealthLoopRunner(WorkerRepository workerRepository,
            @Value("${scheduler.worker-health.heartbeat-timeout-ms:15000}") long heartbeatTimeoutMs) {
        this.workerRepository = workerRepository;
        this.heartbeatTimeoutMs = heartbeatTimeoutMs;
    }

    @Scheduled(fixedDelayString = "${scheduler.worker-health.fixed-delay-ms:5000}")
    public void run() {
        Instant threshold = Instant.now().minusMillis(heartbeatTimeoutMs);
        List<WorkerEntity> stale = workerRepository.findByStatusAndLastHeartbeatBefore(WorkerStatus.HEALTHY, threshold);
        for (WorkerEntity worker : stale) {
            workerRepository.markDown(worker.getId(), threshold);
        }
    }
}
