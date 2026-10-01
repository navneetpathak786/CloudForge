package com.cloudforge.scheduler.scheduling;

import com.cloudforge.scheduler.entity.JobEntity;
import com.cloudforge.scheduler.entity.WorkerEntity;

import java.util.List;
import java.util.Optional;

/**
 * Picks a worker for a job from an already-filtered set of eligible (healthy,
 * sufficient-capacity) workers. Implementations are swappable so different
 * strategies can be compared experimentally (docs/requirements.md section 4/10).
 */
public interface SchedulingStrategy {

    Optional<WorkerEntity> selectWorker(JobEntity job, List<WorkerEntity> eligibleWorkers);
}
