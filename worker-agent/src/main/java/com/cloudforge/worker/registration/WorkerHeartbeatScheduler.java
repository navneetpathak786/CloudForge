package com.cloudforge.worker.registration;

import com.cloudforge.worker.client.SchedulerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WorkerHeartbeatScheduler {

    private static final Logger log = LoggerFactory.getLogger(WorkerHeartbeatScheduler.class);

    private final SchedulerClient schedulerClient;
    private final String workerId;

    public WorkerHeartbeatScheduler(SchedulerClient schedulerClient,
            @Value("${cloudforge.worker.id}") String workerId) {
        this.schedulerClient = schedulerClient;
        this.workerId = workerId;
    }

    @Scheduled(fixedDelayString = "${cloudforge.worker.heartbeat.fixed-delay-ms:5000}")
    public void sendHeartbeat() {
        try {
            schedulerClient.sendHeartbeat(workerId);
        } catch (Exception e) {
            log.warn("Failed to send heartbeat for worker {}", workerId, e);
        }
    }
}
