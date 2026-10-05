package com.cloudforge.worker.registration;

import com.cloudforge.worker.client.SchedulerClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkerRegistrationRunnerTest {

    @Mock
    private SchedulerClient schedulerClient;

    private final WorkerRegistrationState registrationState = new WorkerRegistrationState();

    @Test
    void run_registersConfiguredWorkerWithScheduler() {
        WorkerRegistrationRunner runner =
                new WorkerRegistrationRunner(schedulerClient, registrationState, "worker-1", 4.0, 8192L);

        runner.run(null);

        verify(schedulerClient).registerWorker("worker-1", 4.0, 8192L);
    }

    @Test
    void run_whenRegistrationSucceeds_marksRegistrationState() {
        WorkerRegistrationRunner runner =
                new WorkerRegistrationRunner(schedulerClient, registrationState, "worker-1", 4.0, 8192L);

        runner.run(null);

        assertThat(registrationState.isRegistered()).isTrue();
    }

    @Test
    void run_whenSchedulerCallFails_doesNotPropagateExceptionAndLeavesStateUnregistered() {
        WorkerRegistrationRunner runner =
                new WorkerRegistrationRunner(schedulerClient, registrationState, "worker-1", 4.0, 8192L);
        doThrow(new RuntimeException("scheduler unavailable"))
                .when(schedulerClient).registerWorker("worker-1", 4.0, 8192L);

        runner.run(null);

        assertThat(registrationState.isRegistered()).isFalse();
    }
}
