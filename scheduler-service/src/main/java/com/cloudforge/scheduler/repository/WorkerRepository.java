package com.cloudforge.scheduler.repository;

import com.cloudforge.common.model.WorkerStatus;
import com.cloudforge.scheduler.entity.WorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkerRepository extends JpaRepository<WorkerEntity, String> {

    List<WorkerEntity> findByStatus(WorkerStatus status);

    /**
     * Reserves capacity atomically: the WHERE clause re-checks availability at write
     * time, so a decision made against a possibly-stale read can never overcommit a
     * worker. Returns the number of rows updated (0 if capacity was no longer sufficient).
     */
    @Modifying
    @Query("UPDATE WorkerEntity w SET w.cpuAvailable = w.cpuAvailable - :cpu, "
            + "w.memoryAvailable = w.memoryAvailable - :memory "
            + "WHERE w.id = :id AND w.cpuAvailable >= :cpu AND w.memoryAvailable >= :memory")
    int reserveResources(@Param("id") String id, @Param("cpu") double cpu, @Param("memory") long memory);
}
