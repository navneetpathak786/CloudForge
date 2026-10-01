package com.cloudforge.api.service;

import com.cloudforge.api.dto.JobResponse;
import com.cloudforge.api.dto.JobSubmissionRequest;
import com.cloudforge.api.entity.JobEntity;
import com.cloudforge.api.exception.InvalidJobStateException;
import com.cloudforge.api.exception.JobNotFoundException;
import com.cloudforge.api.queue.JobQueue;
import com.cloudforge.api.repository.JobRepository;
import com.cloudforge.common.model.JobStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobQueue jobQueue;

    @Test
    void submitJob_persistsJobAsQueuedWithGeneratedIdAndTimestampAndEnqueuesIt() {
        JobService jobService = new JobService(jobRepository, jobQueue);

        JobSubmissionRequest request = new JobSubmissionRequest();
        request.setName("nightly-report");
        request.setCommand("run-report.sh");
        request.setCpuRequirement(1.5);
        request.setMemoryRequirement(512L);
        request.setMaxRetries(3);
        request.setPriority(5);

        when(jobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobService.submitJob(request);

        ArgumentCaptor<JobEntity> captor = ArgumentCaptor.forClass(JobEntity.class);
        verify(jobRepository).save(captor.capture());
        JobEntity saved = captor.getValue();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getName()).isEqualTo("nightly-report");
        assertThat(saved.getCommand()).isEqualTo("run-report.sh");
        assertThat(saved.getCpuRequirement()).isEqualTo(1.5);
        assertThat(saved.getMemoryRequirement()).isEqualTo(512L);
        assertThat(saved.getMaxRetries()).isEqualTo(3);
        assertThat(saved.getPriority()).isEqualTo(5);

        assertThat(response.getId()).isEqualTo(saved.getId().toString());
        assertThat(response.getStatus()).isEqualTo(JobStatus.QUEUED);

        verify(jobQueue).enqueue(saved.getId().toString());
    }

    @Test
    void submitJob_whenEnqueueFails_stillPersistsJobAsQueuedAndReturnsSuccessfully() {
        JobService jobService = new JobService(jobRepository, jobQueue);

        JobSubmissionRequest request = new JobSubmissionRequest();
        request.setName("nightly-report");
        request.setCommand("run-report.sh");
        request.setCpuRequirement(1.5);
        request.setMemoryRequirement(512L);
        request.setMaxRetries(3);
        request.setPriority(5);

        when(jobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("redis unavailable")).when(jobQueue).enqueue(any());

        JobResponse response = jobService.submitJob(request);

        assertThat(response.getStatus()).isEqualTo(JobStatus.QUEUED);
    }

    @Test
    void getJob_withExistingId_returnsMappedResponse() {
        JobService jobService = new JobService(jobRepository, jobQueue);

        UUID id = UUID.randomUUID();
        JobEntity entity = new JobEntity();
        entity.setId(id);
        entity.setName("nightly-report");
        entity.setCommand("run-report.sh");
        entity.setCpuRequirement(1.5);
        entity.setMemoryRequirement(512L);
        entity.setMaxRetries(3);
        entity.setPriority(5);
        entity.setStatus(JobStatus.SUBMITTED);
        entity.setCreatedAt(Instant.now());

        when(jobRepository.findById(id)).thenReturn(Optional.of(entity));

        JobResponse response = jobService.getJob(id);

        assertThat(response.getId()).isEqualTo(id.toString());
        assertThat(response.getName()).isEqualTo("nightly-report");
        assertThat(response.getStatus()).isEqualTo(JobStatus.SUBMITTED);
    }

    @Test
    void getJob_withUnknownId_throwsJobNotFoundException() {
        JobService jobService = new JobService(jobRepository, jobQueue);

        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJob(id))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void cancelJob_withQueuedJob_marksJobCancelled() {
        JobService jobService = new JobService(jobRepository, jobQueue);

        UUID id = UUID.randomUUID();
        JobEntity entity = new JobEntity();
        entity.setId(id);
        entity.setStatus(JobStatus.QUEUED);

        when(jobRepository.findById(id)).thenReturn(Optional.of(entity));
        when(jobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobService.cancelJob(id);

        assertThat(response.getStatus()).isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    void cancelJob_withRunningJob_marksJobCancelled() {
        JobService jobService = new JobService(jobRepository, jobQueue);

        UUID id = UUID.randomUUID();
        JobEntity entity = new JobEntity();
        entity.setId(id);
        entity.setStatus(JobStatus.RUNNING);

        when(jobRepository.findById(id)).thenReturn(Optional.of(entity));
        when(jobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobService.cancelJob(id);

        assertThat(response.getStatus()).isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    void cancelJob_withCompletedJob_throwsInvalidJobStateException() {
        JobService jobService = new JobService(jobRepository, jobQueue);

        UUID id = UUID.randomUUID();
        JobEntity entity = new JobEntity();
        entity.setId(id);
        entity.setStatus(JobStatus.COMPLETED);

        when(jobRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> jobService.cancelJob(id))
                .isInstanceOf(InvalidJobStateException.class);
    }

    @Test
    void cancelJob_withUnknownId_throwsJobNotFoundException() {
        JobService jobService = new JobService(jobRepository, jobQueue);

        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.cancelJob(id))
                .isInstanceOf(JobNotFoundException.class);
    }
}
