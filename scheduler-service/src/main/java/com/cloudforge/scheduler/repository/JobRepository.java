package com.cloudforge.scheduler.repository;

import com.cloudforge.common.model.JobStatus;
import com.cloudforge.scheduler.entity.JobEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    /**
     * QUEUED jobs, bounded - the candidate set for Postgres-to-Redis queue
     * reconciliation (Postgres is the source of truth; Redis is a disposable
     * work queue that can lose entries without the job's QUEUED status being lost).
     */
    List<JobEntity> findByStatus(JobStatus status, Limit limit);

    /**
     * Marks a job SCHEDULED atomically: the WHERE clause re-checks that the job is
     * still QUEUED at write time, so a decision made against a possibly-stale read
     * can never overwrite a job that changed state (e.g. was cancelled) in the
     * meantime. Returns the number of rows updated (0 if it was no longer QUEUED).
     */
    @Modifying
    @Query("UPDATE JobEntity j SET j.status = com.cloudforge.common.model.JobStatus.SCHEDULED, "
            + "j.assignedWorkerId = :workerId "
            + "WHERE j.id = :id AND j.status = com.cloudforge.common.model.JobStatus.QUEUED")
    int markScheduled(@Param("id") UUID id, @Param("workerId") String workerId);

    /**
     * Finished jobs whose assigned worker's resources haven't been released yet -
     * the candidate set for the resource-release reconciliation loop.
     */
    List<JobEntity> findByStatusInAndAssignedWorkerIdIsNotNullAndResourcesReleasedAtIsNull(List<JobStatus> statuses);

    /**
     * Marks a job's resources released atomically: the WHERE clause re-checks that
     * they haven't already been released, so a job can never have its worker
     * capacity released twice. Returns the number of rows updated (0 if already released).
     */
    @Modifying
    @Query("UPDATE JobEntity j SET j.resourcesReleasedAt = CURRENT_TIMESTAMP "
            + "WHERE j.id = :id AND j.resourcesReleasedAt IS NULL")
    int markResourcesReleased(@Param("id") UUID id);
}
