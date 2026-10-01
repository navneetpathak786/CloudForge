package com.cloudforge.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * Runs a real redis-server binary as a local subprocess for the "local"
 * profile, so development doesn't depend on a Redis server (Docker or
 * otherwise) being available. Not used under any other profile.
 */
@Configuration
@Profile("local")
public class LocalRedisConfig {

    @Bean(destroyMethod = "stop")
    public RedisServer embeddedRedisServer(@Value("${spring.data.redis.port:6379}") int port) throws IOException {
        RedisServer redisServer = RedisServer.newRedisServer().port(port).build();
        redisServer.start();
        return redisServer;
    }
}
