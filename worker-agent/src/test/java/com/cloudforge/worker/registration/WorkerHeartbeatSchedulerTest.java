package com.cloudforge.worker.registration;

import com.cloudforge.worker.client.SchedulerClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkerHeartbeatSchedulerTest {

    @Mock
    private SchedulerClient schedulerClient;

    @Test
    void sendHeartbeat_sendsHeartbeatForConfiguredWorker() {
        WorkerHeartbeatScheduler scheduler = new WorkerHeartbeatScheduler(schedulerClient, "worker-1");

        scheduler.sendHeartbeat();

        verify(schedulerClient).sendHeartbeat("worker-1");
    }

    @Test
    void sendHeartbeat_whenSchedulerCallFails_doesNotPropagateException() {
        WorkerHeartbeatScheduler scheduler = new WorkerHeartbeatScheduler(schedulerClient, "worker-1");
        doThrow(new RuntimeException("scheduler unavailable"))
                .when(schedulerClient).sendHeartbeat("worker-1");

        scheduler.sendHeartbeat();
    }
}
