package com.cloudforge.scheduler.service;

import com.cloudforge.common.model.WorkerStatus;
import com.cloudforge.scheduler.dto.WorkerRegistrationRequest;
import com.cloudforge.scheduler.dto.WorkerResponse;
import com.cloudforge.scheduler.entity.WorkerEntity;
import com.cloudforge.scheduler.exception.WorkerNotFoundException;
import com.cloudforge.scheduler.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Handles worker registration and heartbeats: persists a worker's capacity
 * under its own self-supplied ID (create-or-update), and records liveness
 * pushes from already-registered workers. Scheduling and stale-heartbeat
 * failure detection are not implemented yet, so a heartbeat unconditionally
 * marks the worker HEALTHY - nothing yet drives it to UNHEALTHY/DOWN.
 */
@Service
public class WorkerService {

    private final WorkerRepository workerRepository;

    public WorkerService(WorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
    }

    @Transactional
    public WorkerRegistrationResult registerWorker(String id, WorkerRegistrationRequest request) {
        WorkerEntity entity = workerRepository.findById(id).orElse(null);
        boolean alreadyRegistered = entity != null;

        if (entity == null) {
            entity = new WorkerEntity();
            entity.setId(id);
            entity.setRegisteredAt(Instant.now());
        }

        entity.setCpuCapacity(request.getCpuCapacity());
        entity.setMemoryCapacity(request.getMemoryCapacity());
        entity.setCpuAvailable(request.getCpuCapacity());
        entity.setMemoryAvailable(request.getMemoryCapacity());
        entity.setStatus(WorkerStatus.REGISTERING);

        WorkerEntity saved = workerRepository.save(entity);
        return new WorkerRegistrationResult(toResponse(saved), alreadyRegistered);
    }

    @Transactional
    public WorkerResponse recordHeartbeat(String id) {
        WorkerEntity entity = workerRepository.findById(id)
                .orElseThrow(() -> new WorkerNotFoundException(id));

        entity.setLastHeartbeat(Instant.now());
        entity.setStatus(WorkerStatus.HEALTHY);

        WorkerEntity saved = workerRepository.save(entity);
        return toResponse(saved);
    }

    private WorkerResponse toResponse(WorkerEntity entity) {
        return new WorkerResponse(
                entity.getId(),
                entity.getCpuCapacity(),
                entity.getMemoryCapacity(),
                entity.getCpuAvailable(),
                entity.getMemoryAvailable(),
                entity.getStatus(),
                entity.getRegisteredAt(),
                entity.getLastHeartbeat());
    }
}
