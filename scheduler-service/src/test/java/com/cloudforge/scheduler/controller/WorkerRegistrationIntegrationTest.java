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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises PUT /api/v1/workers/{id} against the real embedded Postgres (via the
 * "local" profile), matching the project's real-dependency-over-fakes convention.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class WorkerRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkerRepository workerRepository;

    @Test
    void registerWorker_withNewId_persistsWorkerAndReturns201() throws Exception {
        mockMvc.perform(put("/api/v1/workers/{id}", "worker-int-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpuCapacity\": 4.0, \"memoryCapacity\": 8192}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("worker-int-1"))
                .andExpect(jsonPath("$.status").value("REGISTERING"));

        assertThat(workerRepository.findById("worker-int-1")).isPresent();
    }

    @Test
    void registerWorker_withExistingId_updatesCapacityAndReturns200() throws Exception {
        mockMvc.perform(put("/api/v1/workers/{id}", "worker-int-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpuCapacity\": 2.0, \"memoryCapacity\": 4096}"))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/workers/{id}", "worker-int-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpuCapacity\": 8.0, \"memoryCapacity\": 16384}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpuCapacity").value(8.0))
                .andExpect(jsonPath("$.memoryCapacity").value(16384));
    }

    @Test
    void registerWorker_withMissingFields_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/workers/{id}", "worker-int-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cpuCapacity").exists())
                .andExpect(jsonPath("$.errors.memoryCapacity").exists());
    }

    @Test
    void registerWorker_withNonPositiveCapacity_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/workers/{id}", "worker-int-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpuCapacity\": -1, \"memoryCapacity\": 1024}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cpuCapacity").exists());
    }
}
