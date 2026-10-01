package com.cloudforge.worker.client;

import com.cloudforge.worker.dto.JobCompletionRequest;
import com.cloudforge.worker.dto.JobResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void getJob_fetchesJobFromApiServiceEndpoint() {
        JobApiClient client = new JobApiClient(restTemplate, "http://api:8080");
        JobResponse response = new JobResponse();
        response.setId("job-1");
        response.setName("test-job");
        when(restTemplate.getForObject("http://api:8080/api/v1/jobs/{id}", JobResponse.class, "job-1"))
                .thenReturn(response);

        JobResponse result = client.getJob("job-1");

        assertThat(result.getId()).isEqualTo("job-1");
        assertThat(result.getName()).isEqualTo("test-job");
    }

    @Test
    void startJob_postsToApiServiceStartEndpoint() {
        JobApiClient client = new JobApiClient(restTemplate, "http://api:8080");

        client.startJob("job-1");

        verify(restTemplate).postForLocation("http://api:8080/api/v1/jobs/{id}/start", null, "job-1");
    }

    @Test
    void completeJob_postsExitCodeToApiServiceCompleteEndpoint() {
        JobApiClient client = new JobApiClient(restTemplate, "http://api:8080");

        client.completeJob("job-1", 0);

        ArgumentCaptor<JobCompletionRequest> captor = ArgumentCaptor.forClass(JobCompletionRequest.class);
        verify(restTemplate).postForLocation(eq("http://api:8080/api/v1/jobs/{id}/complete"), captor.capture(), eq("job-1"));
        assertThat(captor.getValue().getExitCode()).isEqualTo(0);
    }
}
