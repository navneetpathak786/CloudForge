package com.cloudforge.worker.registration;

import com.cloudforge.worker.client.SchedulerClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkerRegistrationRunnerTest {

    @Mock
    private SchedulerClient schedulerClient;

    @Test
    void run_registersConfiguredWorkerWithScheduler() {
        WorkerRegistrationRunner runner = new WorkerRegistrationRunner(schedulerClient, "worker-1", 4.0, 8192L);

        runner.run(null);

        verify(schedulerClient).registerWorker("worker-1", 4.0, 8192L);
    }

    @Test
    void run_whenSchedulerCallFails_doesNotPropagateException() {
        WorkerRegistrationRunner runner = new WorkerRegistrationRunner(schedulerClient, "worker-1", 4.0, 8192L);
        doThrow(new RuntimeException("scheduler unavailable"))
                .when(schedulerClient).registerWorker("worker-1", 4.0, 8192L);

        runner.run(null);
    }
}
