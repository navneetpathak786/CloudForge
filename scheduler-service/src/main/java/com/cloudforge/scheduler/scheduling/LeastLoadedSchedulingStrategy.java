package com.cloudforge.scheduler.scheduling;

import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.entity.WorkerEntity;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks the eligible worker with the most available CPU, spreading load across
 * the pool instead of packing jobs onto the first fit.
 */
@Component
public class LeastLoadedSchedulingStrategy implements SchedulingStrategy {

    @Override
    public Optional<WorkerEntity> selectWorker(JobEntity job, List<WorkerEntity> eligibleWorkers) {
        return eligibleWorkers.stream()
                .filter(w -> w.getCpuAvailable() >= job.getCpuRequirement())
                .min(Comparator.comparingDouble(WorkerEntity::getCpuAvailable));
    }
}
