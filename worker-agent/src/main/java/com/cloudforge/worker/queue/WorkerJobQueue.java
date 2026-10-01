package com.cloudforge.worker.queue;

import com.cloudforge.common.queue.JobQueueKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Reads job ids off the Redis list scheduler-service delivers onto for this
 * worker (cloudforge:worker:{id}:jobs), per JobQueueKeys.
 */
@Component
public class WorkerJobQueue {

    private final StringRedisTemplate redisTemplate;
    private final String workerId;

    public WorkerJobQueue(StringRedisTemplate redisTemplate,
            @Value("${cloudforge.worker.id}") String workerId) {
        this.redisTemplate = redisTemplate;
        this.workerId = workerId;
    }

    public Optional<String> dequeue() {
        return Optional.ofNullable(redisTemplate.opsForList().leftPop(JobQueueKeys.workerJobsKey(workerId)));
    }
}
