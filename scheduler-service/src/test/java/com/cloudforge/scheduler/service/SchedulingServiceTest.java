package com.cloudforge.scheduler.service;

import com.cloudforge.common.model.JobStatus;
import com.cloudforge.common.model.WorkerStatus;
import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.entity.WorkerEntity;
import com.cloudforge.scheduler.queue.JobQueue;
import com.cloudforge.scheduler.repository.JobRepository;
import com.cloudforge.scheduler.repository.WorkerRepository;
import com.cloudforge.scheduler.scheduling.SchedulingStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

    @Mock
    private JobQueue jobQueue;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private SchedulingStrategy schedulingStrategy;

    private SchedulingService schedulingService() {
        return new SchedulingService(jobQueue, jobRepository, workerRepository, schedulingStrategy);
    }

    private JobEntity job(UUID id, JobStatus status) {
        JobEntity job = new JobEntity();
        job.setId(id);
        job.setStatus(status);
        job.setCpuRequirement(1.0);
        job.setMemoryRequirement(512L);
        return job;
    }

    private WorkerEntity worker(String id) {
        WorkerEntity worker = new WorkerEntity();
        worker.setId(id);
        worker.setStatus(WorkerStatus.HEALTHY);
        worker.setCpuCapacity(4.0);
        worker.setMemoryCapacity(8192L);
        worker.setCpuAvailable(4.0);
        worker.setMemoryAvailable(8192L);
        return worker;
    }

    @Test
    void scheduleNext_withEmptyQueue_returnsFalseAndDoesNothingElse() {
        when(jobQueue.dequeue()).thenReturn(Optional.empty());

        boolean result = schedulingService().scheduleNext();

        assertThat(result).isFalse();
        verify(jobRepository, never()).findById(any());
    }

    @Test
    void scheduleNext_withEligibleWorker_reservesResourcesAndMarksJobScheduled() {
        UUID jobId = UUID.randomUUID();
        JobEntity job = job(jobId, JobStatus.QUEUED);
        WorkerEntity chosenWorker = worker("worker-1");

        when(jobQueue.dequeue()).thenReturn(Optional.of(jobId.toString()));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(workerRepository.findByStatus(WorkerStatus.HEALTHY)).thenReturn(List.of(chosenWorker));
        when(schedulingStrategy.selectWorker(eq(job), any())).thenReturn(Optional.of(chosenWorker));
        when(workerRepository.reserveResources(eq("worker-1"), anyDouble(), anyLong())).thenReturn(1);
        when(jobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = schedulingService().scheduleNext();

        assertThat(result).isTrue();
        assertThat(job.getStatus()).isEqualTo(JobStatus.SCHEDULED);
        assertThat(job.getAssignedWorkerId()).isEqualTo("worker-1");
        verify(jobQueue, never()).requeue(any());
    }

    @Test
    void scheduleNext_withNoEligibleWorker_requeuesJobAndLeavesItQueued() {
        UUID jobId = UUID.randomUUID();
        JobEntity job = job(jobId, JobStatus.QUEUED);

        when(jobQueue.dequeue()).thenReturn(Optional.of(jobId.toString()));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(workerRepository.findByStatus(WorkerStatus.HEALTHY)).thenReturn(List.of());
        when(schedulingStrategy.selectWorker(eq(job), any())).thenReturn(Optional.empty());

        boolean result = schedulingService().scheduleNext();

        assertThat(result).isTrue();
        assertThat(job.getStatus()).isEqualTo(JobStatus.QUEUED);
        verify(jobQueue).requeue(jobId.toString());
        verify(jobRepository, never()).save(any());
    }

    @Test
    void scheduleNext_whenReservationLosesRace_requeuesJobWithoutMarkingScheduled() {
        UUID jobId = UUID.randomUUID();
        JobEntity job = job(jobId, JobStatus.QUEUED);
        WorkerEntity chosenWorker = worker("worker-1");

        when(jobQueue.dequeue()).thenReturn(Optional.of(jobId.toString()));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(workerRepository.findByStatus(WorkerStatus.HEALTHY)).thenReturn(List.of(chosenWorker));
        when(schedulingStrategy.selectWorker(eq(job), any())).thenReturn(Optional.of(chosenWorker));
        when(workerRepository.reserveResources(eq("worker-1"), anyDouble(), anyLong())).thenReturn(0);

        boolean result = schedulingService().scheduleNext();

        assertThat(result).isTrue();
        assertThat(job.getStatus()).isEqualTo(JobStatus.QUEUED);
        verify(jobQueue).requeue(jobId.toString());
        verify(jobRepository, never()).save(any());
    }

    @Test
    void scheduleNext_whenDequeuedJobNoLongerQueued_dropsItWithoutRequeueOrSave() {
        UUID jobId = UUID.randomUUID();
        JobEntity job = job(jobId, JobStatus.CANCELLED);

        when(jobQueue.dequeue()).thenReturn(Optional.of(jobId.toString()));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        boolean result = schedulingService().scheduleNext();

        assertThat(result).isTrue();
        verify(jobQueue, never()).requeue(any());
        verify(jobRepository, never()).save(any());
    }
}
