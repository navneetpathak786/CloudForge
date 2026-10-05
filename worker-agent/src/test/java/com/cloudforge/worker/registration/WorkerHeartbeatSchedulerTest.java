package com.cloudforge.worker.registration;

import com.cloudforge.worker.client.SchedulerClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkerHeartbeatSchedulerTest {

    @Mock
    private SchedulerClient schedulerClient;

    private final WorkerRegistrationState registrationState = new WorkerRegistrationState();

    @Test
    void sendHeartbeat_whenRegistered_sendsHeartbeatForConfiguredWorker() {
        registrationState.markRegistered();
        WorkerHeartbeatScheduler scheduler =
                new WorkerHeartbeatScheduler(schedulerClient, registrationState, "worker-1");

        scheduler.sendHeartbeat();

        verify(schedulerClient).sendHeartbeat("worker-1");
    }

    @Test
    void sendHeartbeat_whenNotYetRegistered_doesNotCallScheduler() {
        WorkerHeartbeatScheduler scheduler =
                new WorkerHeartbeatScheduler(schedulerClient, registrationState, "worker-1");

        scheduler.sendHeartbeat();

        verify(schedulerClient, never()).sendHeartbeat("worker-1");
    }

    @Test
    void sendHeartbeat_whenSchedulerCallFails_doesNotPropagateException() {
        registrationState.markRegistered();
        WorkerHeartbeatScheduler scheduler =
                new WorkerHeartbeatScheduler(schedulerClient, registrationState, "worker-1");
        doThrow(new RuntimeException("scheduler unavailable"))
                .when(schedulerClient).sendHeartbeat("worker-1");

        scheduler.sendHeartbeat();
    }
}
