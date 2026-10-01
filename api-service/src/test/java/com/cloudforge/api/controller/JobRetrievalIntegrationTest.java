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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises GET /api/v1/jobs/{id} against the real embedded Postgres (via the
 * "local" profile), matching the project's real-dependency-over-fakes convention.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class JobRetrievalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Test
    void getJob_withExistingId_returns200WithJobDetails() throws Exception {
        JobEntity entity = new JobEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("nightly-report");
        entity.setCommand("run-report.sh");
        entity.setCpuRequirement(1.5);
        entity.setMemoryRequirement(512L);
        entity.setMaxRetries(3);
        entity.setPriority(5);
        entity.setStatus(JobStatus.SUBMITTED);
        entity.setCreatedAt(Instant.now());
        jobRepository.save(entity);

        mockMvc.perform(get("/api/v1/jobs/{id}", entity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(entity.getId().toString()))
                .andExpect(jsonPath("$.name").value("nightly-report"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void getJob_withUnknownId_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/jobs/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getJob_withMalformedId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
