package com.cloudforge.worker.client;

import com.cloudforge.worker.dto.WorkerRegistrationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Talks to the scheduler-service worker API contracts (PUT /api/v1/workers/{id},
 * POST /api/v1/workers/{id}/heartbeat) exactly as scheduler-service's WorkerController exposes them.
 */
@Component
public class SchedulerClient {

    private final RestTemplate restTemplate;
    private final String schedulerUrl;

    public SchedulerClient(RestTemplate restTemplate, @Value("${cloudforge.scheduler.url}") String schedulerUrl) {
        this.restTemplate = restTemplate;
        this.schedulerUrl = schedulerUrl;
    }

    public void registerWorker(String workerId, double cpuCapacity, long memoryCapacity) {
        WorkerRegistrationRequest request = new WorkerRegistrationRequest();
        request.setCpuCapacity(cpuCapacity);
        request.setMemoryCapacity(memoryCapacity);
        restTemplate.put(schedulerUrl + "/api/v1/workers/{id}", request, workerId);
    }

    public void sendHeartbeat(String workerId) {
        restTemplate.postForLocation(schedulerUrl + "/api/v1/workers/{id}/heartbeat", null, workerId);
    }
}
