package com.cloudforge.api.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Runs a real Postgres binary as a local subprocess for the "local" profile,
 * so development doesn't depend on a Postgres server (Docker or otherwise)
 * being available. Not used under any other profile. Bound to the same
 * DB_PORT other services default to, so another process (e.g. a sibling
 * service run without the "local" profile) can connect to it too.
 */
@Configuration
@Profile("local")
public class LocalDatabaseConfig {

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres(@Value("${DB_PORT:5432}") int port) throws IOException {
        return EmbeddedPostgres.builder().setPort(port).start();
    }

    @Bean
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) throws IOException {
        return embeddedPostgres.getPostgresDatabase();
    }
}
