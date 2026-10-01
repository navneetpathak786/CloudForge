package com.cloudforge.scheduler.controller;

import com.cloudforge.scheduler.dto.WorkerRegistrationRequest;
import com.cloudforge.scheduler.dto.WorkerResponse;
import com.cloudforge.scheduler.service.WorkerRegistrationResult;
import com.cloudforge.scheduler.service.WorkerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workers")
public class WorkerController {

    private final WorkerService workerService;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkerResponse> registerWorker(@PathVariable("id") String id,
                                                           @Valid @RequestBody WorkerRegistrationRequest request) {
        WorkerRegistrationResult result = workerService.registerWorker(id, request);
        HttpStatus status = result.alreadyRegistered() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.response());
    }

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<WorkerResponse> heartbeat(@PathVariable("id") String id) {
        return ResponseEntity.ok(workerService.recordHeartbeat(id));
    }
}
