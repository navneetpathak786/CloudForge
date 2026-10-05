package com.cloudforge.worker.registration;

import com.cloudforge.worker.client.SchedulerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class WorkerRegistrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkerRegistrationRunner.class);

    private final SchedulerClient schedulerClient;
    private final WorkerRegistrationState registrationState;
    private final String workerId;
    private final double cpuCapacity;
    private final long memoryCapacity;

    public WorkerRegistrationRunner(SchedulerClient schedulerClient,
            WorkerRegistrationState registrationState,
            @Value("${cloudforge.worker.id}") String workerId,
            @Value("${cloudforge.worker.cpu-capacity}") double cpuCapacity,
            @Value("${cloudforge.worker.memory-capacity}") long memoryCapacity) {
        this.schedulerClient = schedulerClient;
        this.registrationState = registrationState;
        this.workerId = workerId;
        this.cpuCapacity = cpuCapacity;
        this.memoryCapacity = memoryCapacity;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            schedulerClient.registerWorker(workerId, cpuCapacity, memoryCapacity);
            registrationState.markRegistered();
        } catch (Exception e) {
            log.warn("Failed to register worker {} with the scheduler", workerId, e);
        }
    }
}
