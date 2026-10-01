package com.cloudforge.scheduler.scheduling;

import com.cloudforge.common.model.WorkerStatus;
import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.entity.WorkerEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LeastLoadedSchedulingStrategyTest {

    private final LeastLoadedSchedulingStrategy strategy = new LeastLoadedSchedulingStrategy();

    private WorkerEntity worker(String id, double cpuAvailable) {
        WorkerEntity worker = new WorkerEntity();
        worker.setId(id);
        worker.setCpuCapacity(cpuAvailable);
        worker.setMemoryCapacity(8192L);
        worker.setCpuAvailable(cpuAvailable);
        worker.setMemoryAvailable(8192L);
        worker.setStatus(WorkerStatus.HEALTHY);
        return worker;
    }

    @Test
    void selectWorker_withMultipleEligibleWorkers_picksTheOneWithMostAvailableCpu() {
        JobEntity job = new JobEntity();
        job.setId(UUID.randomUUID());
        job.setCpuRequirement(1.0);
        job.setMemoryRequirement(512L);

        WorkerEntity leastLoaded = worker("worker-2", 6.0);
        List<WorkerEntity> eligible = List.of(worker("worker-1", 2.0), leastLoaded, worker("worker-3", 4.0));

        Optional<WorkerEntity> chosen = strategy.selectWorker(job, eligible);

        assertThat(chosen).contains(leastLoaded);
    }

    @Test
    void selectWorker_withNoEligibleWorkers_returnsEmpty() {
        JobEntity job = new JobEntity();
        job.setId(UUID.randomUUID());
        job.setCpuRequirement(1.0);
        job.setMemoryRequirement(512L);

        Optional<WorkerEntity> chosen = strategy.selectWorker(job, List.of());

        assertThat(chosen).isEmpty();
    }
}
