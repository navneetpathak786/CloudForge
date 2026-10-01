package com.cloudforge.scheduler.queue;

import com.cloudforge.common.queue.JobQueueKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Consumes QUEUED job ids from the Redis list api-service publishes onto.
 */
@Component
public class JobQueue {

    private final StringRedisTemplate redisTemplate;

    public JobQueue(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<String> dequeue() {
        return Optional.ofNullable(redisTemplate.opsForList().leftPop(JobQueueKeys.QUEUED_JOBS_KEY));
    }

    /**
     * Puts a job id back on the queue - used when a scheduling pass can't place it
     * (e.g. no eligible worker yet), so it's retried rather than dropped.
     */
    public void requeue(String jobId) {
        redisTemplate.opsForList().rightPush(JobQueueKeys.QUEUED_JOBS_KEY, jobId);
    }

    public long size() {
        Long size = redisTemplate.opsForList().size(JobQueueKeys.QUEUED_JOBS_KEY);
        return size == null ? 0L : size;
    }
}
