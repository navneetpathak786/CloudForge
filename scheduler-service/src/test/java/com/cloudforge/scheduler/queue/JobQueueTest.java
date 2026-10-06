package com.cloudforge.scheduler.queue;

import com.cloudforge.common.queue.JobQueueKeys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises isQueued() against the real embedded Redis (5.0.14), per the
 * project's real-dependency-over-fakes convention - this is what caught the
 * LPOS incompatibility, since mocked JobQueue usage elsewhere never issues a
 * real Redis command. @AutoConfigureMockMvc is kept here (even though MockMvc
 * is unused) purely so this class's merged context config matches the other
 * "local" profile tests exactly - otherwise Spring boots a second context
 * with its own embedded Redis instance that collides on the same port.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class JobQueueTest {

    @Autowired
    private JobQueue jobQueue;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void isQueued_withJobIdOnTheQueue_returnsTrue() {
        String jobId = UUID.randomUUID().toString();
        redisTemplate.opsForList().rightPush(JobQueueKeys.QUEUED_JOBS_KEY, jobId);

        assertThat(jobQueue.isQueued(jobId)).isTrue();
    }

    @Test
    void isQueued_withJobIdNotOnTheQueue_returnsFalse() {
        assertThat(jobQueue.isQueued(UUID.randomUUID().toString())).isFalse();
    }
}
