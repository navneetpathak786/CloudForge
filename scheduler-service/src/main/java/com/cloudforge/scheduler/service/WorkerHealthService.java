package com.cloudforge.scheduler.service;

import com.cloudforge.scheduler.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Marks a stale worker DOWN. A @Modifying repository query needs an active
 * transaction to execute, so this gives WorkerHealthLoopRunner's per-worker
 * call its own short-lived transaction - the same role ResourceReleaseService
 * and StuckJobRecoveryService play for their own atomic updates.
 */
@Service
public class WorkerHealthService {

    private final WorkerRepository workerRepository;

    public WorkerHealthService(WorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
    }

    @Transactional
    public void markDown(String workerId, Instant threshold) {
        workerRepository.markDown(workerId, threshold);
    }
}
