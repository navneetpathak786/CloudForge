package com.cloudforge.api.controller;

import com.cloudforge.api.entity.JobEntity;
import com.cloudforge.api.repository.JobRepository;
import com.cloudforge.common.model.JobStatus;
import com.cloudforge.common.queue.JobQueueKeys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises POST /api/v1/jobs/{id}/start and /complete against the real embedded
 * Postgres (via the "local" profile), matching the project's real-dependency-over-fakes
 * convention.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class JobExecutionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void startJob_withScheduledJob_returns200AndMarksRunning() throws Exception {
        JobEntity entity = persistJob(JobStatus.SCHEDULED, 3);

        mockMvc.perform(post("/api/v1/jobs/{id}/start", entity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        assertThat(jobRepository.findById(entity.getId()).orElseThrow().getStatus())
                .isEqualTo(JobStatus.RUNNING);
    }

    @Test
    void startJob_withNonScheduledJob_returns409() throws Exception {
        JobEntity entity = persistJob(JobStatus.QUEUED, 3);

        mockMvc.perform(post("/api/v1/jobs/{id}/start", entity.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void completeJob_withZeroExitCode_returns200AndMarksCompleted() throws Exception {
        JobEntity entity = persistJob(JobStatus.RUNNING, 3);

        mockMvc.perform(post("/api/v1/jobs/{id}/complete", entity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exitCode\": 0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(jobRepository.findById(entity.getId()).orElseThrow().getStatus())
                .isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void completeJob_withNonZeroExitCodeAndRetriesRemaining_returns200AndRequeuesForRetry() throws Exception {
        JobEntity entity = persistJob(JobStatus.RUNNING, 3);

        mockMvc.perform(post("/api/v1/jobs/{id}/complete", entity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exitCode\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.attemptCount").value(1));

        JobEntity updated = jobRepository.findById(entity.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(updated.getAttemptCount()).isEqualTo(1);

        List<String> queued = redisTemplate.opsForList().range(JobQueueKeys.QUEUED_JOBS_KEY, 0, -1);
        assertThat(queued).contains(entity.getId().toString());
    }

    @Test
    void completeJob_withNonZeroExitCodeAndRetriesExhausted_returns200AndMarksFailed() throws Exception {
        JobEntity entity = persistJob(JobStatus.RUNNING, 0);

        mockMvc.perform(post("/api/v1/jobs/{id}/complete", entity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exitCode\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.attemptCount").value(1));

        assertThat(jobRepository.findById(entity.getId()).orElseThrow().getStatus())
                .isEqualTo(JobStatus.FAILED);
    }

    @Test
    void completeJob_withNonRunningJob_returns409() throws Exception {
        JobEntity entity = persistJob(JobStatus.SCHEDULED, 3);

        mockMvc.perform(post("/api/v1/jobs/{id}/complete", entity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exitCode\": 0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());

        assertThat(jobRepository.findById(entity.getId()).orElseThrow().getAttemptCount()).isEqualTo(0);
    }

    private JobEntity persistJob(JobStatus status, int maxRetries) {
        JobEntity entity = new JobEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("nightly-report");
        entity.setCommand("run-report.sh");
        entity.setCpuRequirement(1.5);
        entity.setMemoryRequirement(512L);
        entity.setMaxRetries(maxRetries);
        entity.setAttemptCount(0);
        entity.setPriority(5);
        entity.setStatus(status);
        entity.setCreatedAt(Instant.now());
        return jobRepository.save(entity);
    }
}
