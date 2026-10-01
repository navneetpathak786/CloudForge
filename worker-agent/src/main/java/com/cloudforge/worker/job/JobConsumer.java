package com.cloudforge.worker.job;

import com.cloudforge.worker.client.JobApiClient;
import com.cloudforge.worker.dto.JobResponse;
import com.cloudforge.worker.queue.WorkerJobQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Polls this worker's dedicated Redis queue for job ids delivered by the
 * scheduler, and fetches the job's details from api-service. Execution of
 * the fetched job is not yet implemented.
 */
@Component
public class JobConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobConsumer.class);

    private final WorkerJobQueue workerJobQueue;
    private final JobApiClient jobApiClient;
    private final JobExecutor jobExecutor;

    public JobConsumer(WorkerJobQueue workerJobQueue, JobApiClient jobApiClient, JobExecutor jobExecutor) {
        this.workerJobQueue = workerJobQueue;
        this.jobApiClient = jobApiClient;
        this.jobExecutor = jobExecutor;
    }

    @Scheduled(fixedDelayString = "${cloudforge.worker.job-poll.fixed-delay-ms:2000}")
    public void pollForJob() {
        try {
            Optional<String> jobIdOpt = workerJobQueue.dequeue();
            if (jobIdOpt.isEmpty()) {
                return;
            }

            String jobId = jobIdOpt.get();
            JobResponse job = jobApiClient.getJob(jobId);
            log.info("Received job {} ({}) for execution", job.getId(), job.getName());
            jobExecutor.execute(job);
        } catch (Exception e) {
            log.warn("Failed to poll/consume job from worker queue", e);
        }
    }
}
