package com.cloudforge.scheduler.repository;

import com.cloudforge.common.model.WorkerStatus;
import com.cloudforge.scheduler.entity.WorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface WorkerRepository extends JpaRepository<WorkerEntity, String> {

    List<WorkerEntity> findByStatus(WorkerStatus status);

    /**
     * HEALTHY workers whose last heartbeat is older than the given threshold -
     * the candidate set for the worker-health sweep. A null lastHeartbeat (never
     * heartbeated yet, still REGISTERING) never matches "<", so a freshly
     * registered worker isn't mistaken for a stale one.
     */
    List<WorkerEntity> findByStatusAndLastHeartbeatBefore(WorkerStatus status, Instant threshold);

    /**
     * Marks a worker DOWN atomically: the WHERE clause re-checks it's still
     * HEALTHY and still stale as of the same threshold at write time, so a
     * heartbeat that arrives concurrently with the sweep can't be clobbered.
     * Returns the number of rows updated (0 if it was no longer stale/HEALTHY).
     */
    @Modifying
    @Query("UPDATE WorkerEntity w SET w.status = com.cloudforge.common.model.WorkerStatus.DOWN "
            + "WHERE w.id = :id AND w.status = com.cloudforge.common.model.WorkerStatus.HEALTHY "
            + "AND w.lastHeartbeat < :threshold")
    int markDown(@Param("id") String id, @Param("threshold") Instant threshold);

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

    /**
     * Releases a reservation previously made by reserveResources - used when the
     * job it was reserved for turned out to have changed state before scheduling
     * could be committed, so the capacity isn't lost.
     */
    @Modifying
    @Query("UPDATE WorkerEntity w SET w.cpuAvailable = w.cpuAvailable + :cpu, "
            + "w.memoryAvailable = w.memoryAvailable + :memory WHERE w.id = :id")
    int releaseResources(@Param("id") String id, @Param("cpu") double cpu, @Param("memory") long memory);
}
