package com.cloudforge.api.controller;

import com.cloudforge.api.entity.JobEntity;
import com.cloudforge.api.repository.JobRepository;
import com.cloudforge.common.model.JobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
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

    @Test
    void startJob_withScheduledJob_returns200AndMarksRunning() throws Exception {
        JobEntity entity = persistJob(JobStatus.SCHEDULED);

        mockMvc.perform(post("/api/v1/jobs/{id}/start", entity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        assertThat(jobRepository.findById(entity.getId()).orElseThrow().getStatus())
                .isEqualTo(JobStatus.RUNNING);
    }

    @Test
    void startJob_withNonScheduledJob_returns409() throws Exception {
        JobEntity entity = persistJob(JobStatus.QUEUED);

        mockMvc.perform(post("/api/v1/jobs/{id}/start", entity.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void completeJob_withZeroExitCode_returns200AndMarksCompleted() throws Exception {
        JobEntity entity = persistJob(JobStatus.RUNNING);

        mockMvc.perform(post("/api/v1/jobs/{id}/complete", entity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exitCode\": 0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(jobRepository.findById(entity.getId()).orElseThrow().getStatus())
                .isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void completeJob_withNonZeroExitCode_returns200AndMarksFailed() throws Exception {
        JobEntity entity = persistJob(JobStatus.RUNNING);

        mockMvc.perform(post("/api/v1/jobs/{id}/complete", entity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exitCode\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        assertThat(jobRepository.findById(entity.getId()).orElseThrow().getStatus())
                .isEqualTo(JobStatus.FAILED);
    }

    @Test
    void completeJob_withNonRunningJob_returns409() throws Exception {
        JobEntity entity = persistJob(JobStatus.SCHEDULED);

        mockMvc.perform(post("/api/v1/jobs/{id}/complete", entity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exitCode\": 0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    private JobEntity persistJob(JobStatus status) {
        JobEntity entity = new JobEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("nightly-report");
        entity.setCommand("run-report.sh");
        entity.setCpuRequirement(1.5);
        entity.setMemoryRequirement(512L);
        entity.setMaxRetries(3);
        entity.setPriority(5);
        entity.setStatus(status);
        entity.setCreatedAt(Instant.now());
        return jobRepository.save(entity);
    }
}
