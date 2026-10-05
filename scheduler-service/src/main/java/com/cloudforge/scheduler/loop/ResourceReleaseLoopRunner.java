package com.cloudforge.scheduler.loop;

import com.cloudforge.common.model.JobStatus;
import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.repository.JobRepository;
import com.cloudforge.scheduler.service.ResourceReleaseService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically reconciles COMPLETED/FAILED jobs against the jobs table and
 * releases their assigned worker's reserved capacity. This is how resources get
 * returned, since api-service (which writes COMPLETED/FAILED) has no connection
 * to scheduler-service's worker resource tracking.
 */
@Component
public class ResourceReleaseLoopRunner {

    private static final List<JobStatus> FINISHED_STATUSES = List.of(JobStatus.COMPLETED, JobStatus.FAILED);

    private final ResourceReleaseService resourceReleaseService;
    private final JobRepository jobRepository;

    public ResourceReleaseLoopRunner(ResourceReleaseService resourceReleaseService, JobRepository jobRepository) {
        this.resourceReleaseService = resourceReleaseService;
        this.jobRepository = jobRepository;
    }

    @Scheduled(fixedDelayString = "${scheduler.resource-release.fixed-delay-ms:5000}")
    public void run() {
        List<JobEntity> pending =
                jobRepository.findByStatusInAndAssignedWorkerIdIsNotNullAndResourcesReleasedAtIsNull(FINISHED_STATUSES);
        for (JobEntity job : pending) {
            resourceReleaseService.release(job);
        }
    }
}
