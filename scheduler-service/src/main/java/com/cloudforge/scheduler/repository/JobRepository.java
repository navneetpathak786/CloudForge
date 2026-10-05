package com.cloudforge.scheduler.repository;

import com.cloudforge.scheduler.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {

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
}
