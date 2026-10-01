package com.cloudforge.api.controller;

import com.cloudforge.api.dto.JobResponse;
import com.cloudforge.api.dto.JobSubmissionRequest;
import com.cloudforge.api.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> submitJob(@Valid @RequestBody JobSubmissionRequest request) {
        JobResponse response = jobService.submitJob(request);
        return ResponseEntity.created(URI.create("/api/v1/jobs/" + response.getId())).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(jobService.getJob(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<JobResponse> cancelJob(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(jobService.cancelJob(id));
    }
}
