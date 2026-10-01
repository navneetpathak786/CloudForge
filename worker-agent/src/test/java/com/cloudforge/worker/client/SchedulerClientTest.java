package com.cloudforge.worker.client;

import com.cloudforge.worker.dto.WorkerRegistrationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SchedulerClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void registerWorker_putsRequestOntoSchedulerWorkerEndpoint() {
        SchedulerClient client = new SchedulerClient(restTemplate, "http://scheduler:8081");

        client.registerWorker("worker-1", 4.0, 8192L);

        ArgumentCaptor<WorkerRegistrationRequest> captor = ArgumentCaptor.forClass(WorkerRegistrationRequest.class);
        verify(restTemplate).put(eq("http://scheduler:8081/api/v1/workers/{id}"), captor.capture(), eq("worker-1"));

        WorkerRegistrationRequest body = captor.getValue();
        assertThat(body.getCpuCapacity()).isEqualTo(4.0);
        assertThat(body.getMemoryCapacity()).isEqualTo(8192L);
    }

    @Test
    void sendHeartbeat_postsToSchedulerHeartbeatEndpoint() {
        SchedulerClient client = new SchedulerClient(restTemplate, "http://scheduler:8081");

        client.sendHeartbeat("worker-1");

        verify(restTemplate).postForLocation("http://scheduler:8081/api/v1/workers/{id}/heartbeat", null, "worker-1");
    }
}
