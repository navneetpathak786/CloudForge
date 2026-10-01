package com.cloudforge.api.controller;

import com.cloudforge.api.entity.JobEntity;
import com.cloudforge.api.repository.JobRepository;
import com.cloudforge.common.model.JobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises POST /api/v1/jobs/{id}/cancel against the real embedded Postgres (via the
 * "local" profile), matching the project's real-dependency-over-fakes convention.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class JobCancellationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Test
    void cancelJob_withQueuedJob_returns200AndMarksCancelled() throws Exception {
        JobEntity entity = persistJob(JobStatus.QUEUED);

        mockMvc.perform(post("/api/v1/jobs/{id}/cancel", entity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(jobRepository.findById(entity.getId()).orElseThrow().getStatus())
                .isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    void cancelJob_withRunningJob_returns200AndMarksCancelled() throws Exception {
        JobEntity entity = persistJob(JobStatus.RUNNING);

        mockMvc.perform(post("/api/v1/jobs/{id}/cancel", entity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(jobRepository.findById(entity.getId()).orElseThrow().getStatus())
                .isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    void cancelJob_withCompletedJob_returns409() throws Exception {
        JobEntity entity = persistJob(JobStatus.COMPLETED);

        mockMvc.perform(post("/api/v1/jobs/{id}/cancel", entity.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());

        assertThat(jobRepository.findById(entity.getId()).orElseThrow().getStatus())
                .isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void cancelJob_withUnknownId_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/{id}/cancel", UUID.randomUUID()))
                .andExpect(status().isNotFound())
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
