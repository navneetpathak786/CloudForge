package com.cloudforge.scheduler.controller;

import com.cloudforge.scheduler.repository.WorkerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises POST /api/v1/workers/{id}/heartbeat against the real embedded Postgres
 * (via the "local" profile), matching the project's real-dependency-over-fakes convention.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class WorkerHeartbeatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkerRepository workerRepository;

    @Test
    void heartbeat_withRegisteredWorker_returns200AndMarksHealthy() throws Exception {
        mockMvc.perform(put("/api/v1/workers/{id}", "worker-hb-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpuCapacity\": 4.0, \"memoryCapacity\": 8192}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/workers/{id}/heartbeat", "worker-hb-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HEALTHY"))
                .andExpect(jsonPath("$.lastHeartbeat").exists());

        assertThat(workerRepository.findById("worker-hb-1").orElseThrow().getLastHeartbeat())
                .isNotNull();
    }

    @Test
    void heartbeat_withUnknownId_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/workers/{id}/heartbeat", "unknown-worker"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}
