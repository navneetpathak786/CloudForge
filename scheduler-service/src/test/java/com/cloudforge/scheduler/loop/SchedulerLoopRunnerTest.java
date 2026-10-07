package com.cloudforge.scheduler.loop;

import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.queue.JobQueue;
import com.cloudforge.scheduler.repository.JobRepository;
import com.cloudforge.scheduler.service.SchedulingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerLoopRunnerTest {

    @Mock
    private SchedulingService schedulingService;

    @Mock
    private JobQueue jobQueue;

    @Mock
    private JobRepository jobRepository;

    private JobEntity job(UUID id, Integer priority) {
        JobEntity job = new JobEntity();
        job.setId(id);
        job.setPriority(priority);
        return job;
    }

    @Test
    void run_withHigherAndLowerPriorityJobs_requeuesHigherPriorityFirst() {
        SchedulerLoopRunner runner = new SchedulerLoopRunner(schedulingService, jobQueue, jobRepository);
        UUID lowId = UUID.randomUUID();
        UUID highId = UUID.randomUUID();

        when(jobQueue.size()).thenReturn(2L);
        when(jobQueue.dequeue()).thenReturn(Optional.of(lowId.toString()), Optional.of(highId.toString()));
        when(jobRepository.findAllById(List.of(lowId, highId)))
                .thenReturn(List.of(job(lowId, 1), job(highId, 10)));

        runner.run();

        InOrder order = inOrder(jobQueue);
        order.verify(jobQueue).requeue(highId.toString());
        order.verify(jobQueue).requeue(lowId.toString());
        verify(schedulingService, times(2)).scheduleNext();
    }

    @Test
    void run_withEqualPriorityJobs_preservesOriginalArrivalOrder() {
        SchedulerLoopRunner runner = new SchedulerLoopRunner(schedulingService, jobQueue, jobRepository);
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        when(jobQueue.size()).thenReturn(2L);
        when(jobQueue.dequeue()).thenReturn(Optional.of(firstId.toString()), Optional.of(secondId.toString()));
        when(jobRepository.findAllById(List.of(firstId, secondId)))
                .thenReturn(List.of(job(firstId, 5), job(secondId, 5)));

        runner.run();

        InOrder order = inOrder(jobQueue);
        order.verify(jobQueue).requeue(firstId.toString());
        order.verify(jobQueue).requeue(secondId.toString());
    }

    @Test
    void run_withMixedAndMissingPriorities_stillAttemptsSchedulingForEveryDrainedJob() {
        SchedulerLoopRunner runner = new SchedulerLoopRunner(schedulingService, jobQueue, jobRepository);
        UUID noPriorityId = UUID.randomUUID();
        UUID highId = UUID.randomUUID();

        when(jobQueue.size()).thenReturn(2L);
        when(jobQueue.dequeue()).thenReturn(Optional.of(noPriorityId.toString()), Optional.of(highId.toString()));
        when(jobRepository.findAllById(List.of(noPriorityId, highId)))
                .thenReturn(List.of(job(noPriorityId, null), job(highId, 10)));

        runner.run();

        // Reordering never drops a job - every drained job still gets a scheduling
        // attempt, so SchedulingService's own worker eligibility/capacity checks
        // (unchanged) still run for each one regardless of priority.
        verify(schedulingService, times(2)).scheduleNext();
        InOrder order = inOrder(jobQueue);
        order.verify(jobQueue).requeue(highId.toString());
        order.verify(jobQueue).requeue(noPriorityId.toString());
    }

    @Test
    void byPriorityDescending_sortsDescendingWithNullsLast() {
        SchedulerLoopRunner runner = new SchedulerLoopRunner(schedulingService, jobQueue, jobRepository);
        UUID low = UUID.randomUUID();
        UUID high = UUID.randomUUID();
        UUID none = UUID.randomUUID();

        when(jobRepository.findAllById(List.of(low, high, none)))
                .thenReturn(List.of(job(low, 1), job(high, 10), job(none, null)));

        List<String> ordered = runner.byPriorityDescending(List.of(low.toString(), high.toString(), none.toString()));

        assertThat(ordered).containsExactly(high.toString(), low.toString(), none.toString());
    }
}
