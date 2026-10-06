package com.cloudforge.api.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

    private static final String APP_ROLE = "cloudforge";
    private static final String APP_DATABASE = "cloudforge";

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres(@Value("${DB_PORT:5432}") int port) throws IOException {
        EmbeddedPostgres embeddedPostgres = EmbeddedPostgres.builder().setPort(port).start();
        provisionAppRoleAndDatabase(embeddedPostgres);
        return embeddedPostgres;
    }

    @Bean
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) throws IOException {
        return embeddedPostgres.getDatabase(APP_ROLE, APP_DATABASE);
    }

    /**
     * The embedded instance only has zonky's default "postgres" admin role/database.
     * Sibling services (e.g. scheduler-service, run without this profile) connect
     * using the "cloudforge" role/database per application.yml - the same identity
     * the real docker-compose Postgres provisions via POSTGRES_USER/POSTGRES_DB - so
     * create them here too, once, via the admin connection. Existence is checked
     * first since Postgres has no "CREATE ROLE/DATABASE IF NOT EXISTS", which also
     * makes this safe to run again (e.g. on every app restart) without erroring on
     * an already-provisioned instance. CREATE DATABASE can't run inside a
     * transaction, so this relies on the connection's default autocommit - each
     * statement is its own implicit transaction - rather than an explicit one.
     */
    void provisionAppRoleAndDatabase(EmbeddedPostgres embeddedPostgres) throws IOException {
        try (Connection connection = embeddedPostgres.getPostgresDatabase().getConnection();
             Statement statement = connection.createStatement()) {

            if (!exists(statement, "SELECT 1 FROM pg_roles WHERE rolname = '" + APP_ROLE + "'")) {
                statement.execute("CREATE ROLE " + APP_ROLE + " LOGIN PASSWORD '" + APP_ROLE + "'");
            }

            if (!exists(statement, "SELECT 1 FROM pg_database WHERE datname = '" + APP_DATABASE + "'")) {
                statement.execute("CREATE DATABASE " + APP_DATABASE + " OWNER " + APP_ROLE);
            }
        } catch (SQLException e) {
            throw new IOException("Failed to provision the local '" + APP_ROLE + "' role/database", e);
        }
    }

    private boolean exists(Statement statement, String query) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(query)) {
            return resultSet.next();
        }
    }
}
