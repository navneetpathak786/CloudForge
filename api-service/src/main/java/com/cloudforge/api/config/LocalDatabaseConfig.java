package com.cloudforge.api.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Runs a real Postgres binary as a local subprocess for the "local" profile,
 * so development doesn't depend on a Postgres server (Docker or otherwise)
 * being available. Not used under any other profile.
 */
@Configuration
@Profile("local")
public class LocalDatabaseConfig {

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder().start();
    }

    @Bean
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) throws IOException {
        return embeddedPostgres.getPostgresDatabase();
    }
}
