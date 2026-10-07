package com.cloudforge.scheduler.service;

import com.cloudforge.common.model.JobStatus;
import com.cloudforge.common.model.WorkerStatus;
import com.cloudforge.common.queue.JobQueueKeys;
import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.entity.WorkerEntity;
import com.cloudforge.scheduler.repository.JobRepository;
import com.cloudforge.scheduler.repository.WorkerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises worker failure detection and recovery against real embedded
 * Postgres and Redis, per the project's real-dependency-over-fakes
 * convention. The background @Scheduled loops are disabled test-wide
 * (src/test/resources/application-local.yml) so the detection/recovery steps
 * can be driven and asserted on deterministically, same reasoning as
 * SchedulingIntegrationTest. @AutoConfigureMockMvc is kept here (even though
 * MockMvc is unused) purely so this class's merged context config matches
 * the other "local" profile tests exactly - otherwise Spring boots a second
 * context with its own embedded Postgres/Redis that collides on the same
 * fixed ports with the one already alive from the other test classes'
 * cached context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class WorkerFailureRecoveryIntegrationTest {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private StuckJobRecoveryService stuckJobRecoveryService;

    @Autowired
    private WorkerHealthService workerHealthService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private WorkerEntity persistWorker(String id, WorkerStatus status, Instant lastHeartbeat,
            double cpuAvailable, long memoryAvailable) {
        WorkerEntity worker = new WorkerEntity();
        worker.setId(id);
        worker.setCpuCapacity(cpuAvailable);
        worker.setMemoryCapacity(memoryAvailable);
        worker.setCpuAvailable(cpuAvailable);
        worker.setMemoryAvailable(memoryAvailable);
        worker.setStatus(status);
        worker.setRegisteredAt(Instant.now());
        worker.setLastHeartbeat(lastHeartbeat);
        return workerRepository.save(worker);
    }

    private JobEntity persistRunningJob(String assignedWorkerId, double cpu, long memory) {
        JobEntity job = new JobEntity();
        job.setId(UUID.randomUUID());
        job.setName("stuck-job");
        job.setCommand("echo hi");
        job.setCpuRequirement(cpu);
        job.setMemoryRequirement(memory);
        job.setMaxRetries(0);
        job.setPriority(1);
        job.setStatus(JobStatus.RUNNING);
        job.setAssignedWorkerId(assignedWorkerId);
        job.setCreatedAt(Instant.now());
        return jobRepository.save(job);
    }

    @Test
    void findByStatusAndLastHeartbeatBefore_detectsOnlyWorkersStaleAsOfThreshold() {
        Instant threshold = Instant.now().minusSeconds(15);
        persistWorker("int-stale-worker", WorkerStatus.HEALTHY, threshold.minusSeconds(5), 1.0, 512L);
        persistWorker("int-fresh-worker", WorkerStatus.HEALTHY, Instant.now(), 1.0, 512L);

        List<WorkerEntity> stale = workerRepository.findByStatusAndLastHeartbeatBefore(WorkerStatus.HEALTHY, threshold);

        assertThat(stale).extracting(WorkerEntity::getId).contains("int-stale-worker");
        assertThat(stale).extracting(WorkerEntity::getId).doesNotContain("int-fresh-worker");

        workerHealthService.markDown("int-stale-worker", threshold);
        assertThat(workerRepository.findById("int-stale-worker").orElseThrow().getStatus())
                .isEqualTo(WorkerStatus.DOWN);
        assertThat(workerRepository.findById("int-fresh-worker").orElseThrow().getStatus())
                .isEqualTo(WorkerStatus.HEALTHY);
    }

    @Test
    void recover_withJobStuckOnDownWorker_requeuesJobAndReleasesWorkerCapacity() {
        WorkerEntity worker = persistWorker("int-down-worker", WorkerStatus.DOWN, Instant.now().minusSeconds(60),
                10.0, 2048L);
        JobEntity job = persistRunningJob("int-down-worker", 1.0, 512L);
        redisTemplate.delete(JobQueueKeys.QUEUED_JOBS_KEY);

        stuckJobRecoveryService.recover(job);

        JobEntity updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updatedJob.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(updatedJob.getAssignedWorkerId()).isNull();

        WorkerEntity updatedWorker = workerRepository.findById(worker.getId()).orElseThrow();
        assertThat(updatedWorker.getCpuAvailable()).isEqualTo(11.0);
        assertThat(updatedWorker.getMemoryAvailable()).isEqualTo(2560L);

        assertThat(redisTemplate.opsForList().range(JobQueueKeys.QUEUED_JOBS_KEY, 0, -1))
                .contains(job.getId().toString());
    }
}
