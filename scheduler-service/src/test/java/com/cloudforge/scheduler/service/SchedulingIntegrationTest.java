package com.cloudforge.scheduler.service;

import com.cloudforge.common.model.JobStatus;
import com.cloudforge.common.model.WorkerStatus;
import com.cloudforge.common.queue.JobQueueKeys;
import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.entity.WorkerEntity;
import com.cloudforge.scheduler.repository.JobRepository;
import com.cloudforge.scheduler.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real scheduling path (Redis dequeue -> worker selection ->
 * atomic reservation -> Postgres update) against real embedded Postgres and
 * Redis, per the project's real-dependency-over-fakes convention. The
 * background @Scheduled loop is disabled test-wide (src/test/resources/
 * application-local.yml) so scheduleNext() can be driven and asserted on
 * deterministically. @AutoConfigureMockMvc is kept here (even though MockMvc
 * is unused) purely so this class's merged context config matches the other
 * "local" profile tests exactly - otherwise Spring boots a second context
 * with its own embedded Redis instance that collides on the same port with
 * the one already alive from the other test classes' cached context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SchedulingIntegrationTest {

    @Autowired
    private SchedulingService schedulingService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void clearQueue() {
        redisTemplate.delete(JobQueueKeys.QUEUED_JOBS_KEY);
    }

    private JobEntity persistQueuedJob(double cpu, long memory) {
        JobEntity job = new JobEntity();
        job.setId(UUID.randomUUID());
        job.setName("int-test-job");
        job.setCommand("echo hi");
        job.setCpuRequirement(cpu);
        job.setMemoryRequirement(memory);
        job.setMaxRetries(0);
        job.setPriority(1);
        job.setStatus(JobStatus.QUEUED);
        job.setCreatedAt(Instant.now());
        return jobRepository.save(job);
    }

    private WorkerEntity persistHealthyWorker(String id, double cpuAvailable, long memoryAvailable) {
        WorkerEntity worker = new WorkerEntity();
        worker.setId(id);
        worker.setCpuCapacity(cpuAvailable);
        worker.setMemoryCapacity(memoryAvailable);
        worker.setCpuAvailable(cpuAvailable);
        worker.setMemoryAvailable(memoryAvailable);
        worker.setStatus(WorkerStatus.HEALTHY);
        worker.setRegisteredAt(Instant.now());
        return workerRepository.save(worker);
    }

    @Test
    void scheduleNext_withHealthyEligibleWorker_reservesCapacityAndMarksJobScheduled() {
        // Capacity is deliberately far larger than any worker persisted by other
        // integration test classes sharing this Postgres instance, so this worker
        // is unambiguously the max-available (least-loaded) choice regardless of
        // leftover rows from other tests.
        JobEntity job = persistQueuedJob(1.0, 512L);
        persistHealthyWorker("int-worker-1", 1_000_000.0, 1_000_000_000L);
        redisTemplate.opsForList().rightPush(JobQueueKeys.QUEUED_JOBS_KEY, job.getId().toString());

        boolean result = schedulingService.scheduleNext();

        assertThat(result).isTrue();

        JobEntity updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updatedJob.getStatus()).isEqualTo(JobStatus.SCHEDULED);
        assertThat(updatedJob.getAssignedWorkerId()).isEqualTo("int-worker-1");

        WorkerEntity updatedWorker = workerRepository.findById("int-worker-1").orElseThrow();
        assertThat(updatedWorker.getCpuAvailable()).isEqualTo(999_999.0);
        assertThat(updatedWorker.getMemoryAvailable()).isEqualTo(999_999_488L);

        assertThat(redisTemplate.opsForList().range(JobQueueKeys.workerJobsKey("int-worker-1"), 0, -1))
                .contains(job.getId().toString());
    }

    @Test
    void scheduleNext_withNoEligibleWorker_requeuesJobAndLeavesItQueued() {
        // Requirement is deliberately far larger than any worker's capacity could
        // ever be (including leftover workers from other test classes sharing this
        // Postgres instance), so this job can never find an eligible worker.
        JobEntity job = persistQueuedJob(1_000_000.0, 1_000_000_000L);
        persistHealthyWorker("int-worker-2", 1.0, 1024L);
        redisTemplate.opsForList().rightPush(JobQueueKeys.QUEUED_JOBS_KEY, job.getId().toString());

        boolean result = schedulingService.scheduleNext();

        assertThat(result).isTrue();

        JobEntity updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updatedJob.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(updatedJob.getAssignedWorkerId()).isNull();

        assertThat(redisTemplate.opsForList().range(JobQueueKeys.QUEUED_JOBS_KEY, 0, -1))
                .contains(job.getId().toString());
    }
}
