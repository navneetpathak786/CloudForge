package com.cloudforge.api.controller;

import com.cloudforge.api.repository.JobRepository;
import com.cloudforge.common.queue.JobQueueKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the full submission path (REST -> validation -> real embedded
 * Postgres) using the same "local" profile as local development, per the
 * project's real-dependency-over-fakes convention.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class JobSubmissionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void submitJob_withValidRequest_persistsJobAsQueuedAndEnqueuesItOntoRedis() throws Exception {
        Map<String, Object> request = Map.of(
                "name", "nightly-report",
                "command", "run-report.sh",
                "cpuRequirement", 1.5,
                "memoryRequirement", 512,
                "maxRetries", 3,
                "priority", 5
        );

        String responseBody = mockMvc.perform(post("/api/v1/jobs")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(objectMapper.readTree(responseBody).get("id").asText());

        assertThat(jobRepository.findById(id)).isPresent();
        assertThat(jobRepository.findById(id).get().getName()).isEqualTo("nightly-report");

        List<String> queued = redisTemplate.opsForList().range(JobQueueKeys.QUEUED_JOBS_KEY, 0, -1);
        assertThat(queued).contains(id.toString());
    }

    @Test
    void submitJob_withMissingRequiredFields_returns400WithFieldErrors() throws Exception {
        Map<String, Object> request = Map.of("maxRetries", 3);

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.command").exists())
                .andExpect(jsonPath("$.errors.cpuRequirement").exists())
                .andExpect(jsonPath("$.errors.memoryRequirement").exists());
    }

    @Test
    void submitJob_withNonPositiveCpu_returns400() throws Exception {
        Map<String, Object> request = Map.of(
                "name", "bad-job",
                "command", "echo hi",
                "cpuRequirement", -1,
                "memoryRequirement", 256,
                "maxRetries", 0
        );

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cpuRequirement").exists());
    }
}
