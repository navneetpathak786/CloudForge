package com.cloudforge.api.queue;

import com.cloudforge.common.queue.JobQueueKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes QUEUED job ids onto the Redis list scheduler-service dequeues from.
 */
@Component
public class JobQueue {

    private final StringRedisTemplate redisTemplate;

    public JobQueue(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void enqueue(String jobId) {
        redisTemplate.opsForList().rightPush(JobQueueKeys.QUEUED_JOBS_KEY, jobId);
    }
}
