package com.cloudforge.scheduler.loop;

import com.cloudforge.common.model.JobStatus;
import com.cloudforge.common.model.WorkerStatus;
import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.entity.WorkerEntity;
import com.cloudforge.scheduler.repository.JobRepository;
import com.cloudforge.scheduler.repository.WorkerRepository;
import com.cloudforge.scheduler.service.StuckJobRecoveryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically finds jobs stuck RUNNING on a DOWN worker (see
 * WorkerHealthLoopRunner) and recovers each one.
 */
@Component
@ConditionalOnProperty(name = "scheduler.stuck-job-recovery.enabled", matchIfMissing = true)
public class StuckJobRecoveryLoopRunner {

    private final WorkerRepository workerRepository;
    private final JobRepository jobRepository;
    private final StuckJobRecoveryService stuckJobRecoveryService;

    public StuckJobRecoveryLoopRunner(WorkerRepository workerRepository, JobRepository jobRepository,
            StuckJobRecoveryService stuckJobRecoveryService) {
        this.workerRepository = workerRepository;
        this.jobRepository = jobRepository;
        this.stuckJobRecoveryService = stuckJobRecoveryService;
    }

    @Scheduled(fixedDelayString = "${scheduler.stuck-job-recovery.fixed-delay-ms:5000}")
    public void run() {
        List<WorkerEntity> downWorkers = workerRepository.findByStatus(WorkerStatus.DOWN);
        for (WorkerEntity worker : downWorkers) {
            List<JobEntity> stuckJobs = jobRepository.findByStatusAndAssignedWorkerId(JobStatus.RUNNING, worker.getId());
            for (JobEntity job : stuckJobs) {
                stuckJobRecoveryService.recover(job);
            }
        }
    }
}
