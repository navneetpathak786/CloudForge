package com.cloudforge.worker.client;

import com.cloudforge.worker.dto.JobCompletionRequest;
import com.cloudforge.worker.dto.JobResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Talks to api-service's job API contracts (GET /api/v1/jobs/{id},
 * POST /api/v1/jobs/{id}/start, POST /api/v1/jobs/{id}/complete) exactly as
 * api-service's JobController exposes them.
 */
@Component
public class JobApiClient {

    private final RestTemplate restTemplate;
    private final String apiUrl;

    public JobApiClient(RestTemplate restTemplate, @Value("${cloudforge.api.url}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
    }

    public JobResponse getJob(String jobId) {
        return restTemplate.getForObject(apiUrl + "/api/v1/jobs/{id}", JobResponse.class, jobId);
    }

    public void startJob(String jobId) {
        restTemplate.postForLocation(apiUrl + "/api/v1/jobs/{id}/start", null, jobId);
    }

    public void completeJob(String jobId, int exitCode) {
        JobCompletionRequest request = new JobCompletionRequest();
        request.setExitCode(exitCode);
        restTemplate.postForLocation(apiUrl + "/api/v1/jobs/{id}/complete", request, jobId);
    }
}
