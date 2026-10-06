package com.cloudforge.api.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies the "local" profile's embedded Postgres ends up with a usable
 * "cloudforge" role/database - the identity sibling services (e.g.
 * scheduler-service, run without this profile) connect with per
 * application.yml - and that provisioning it is safe to repeat.
 *
 * @AutoConfigureMockMvc matches every other "local"-profile @SpringBootTest
 * in this module purely so this test reuses their cached ApplicationContext
 * (and its single embedded Postgres/Redis pair bound to fixed ports) instead
 * of booting a second one under a different cache key.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalDatabaseConfigTest {

    @Autowired
    private EmbeddedPostgres embeddedPostgres;

    @Autowired
    private LocalDatabaseConfig localDatabaseConfig;

    @Test
    void embeddedPostgres_hasConnectableCloudforgeRoleAndDatabase() throws Exception {
        DataSource cloudforgeDataSource = embeddedPostgres.getDatabase("cloudforge", "cloudforge");

        try (Connection connection = cloudforgeDataSource.getConnection();
             ResultSet resultSet = connection.createStatement()
                     .executeQuery("SELECT current_database(), current_user")) {

            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString(1)).isEqualTo("cloudforge");
            assertThat(resultSet.getString(2)).isEqualTo("cloudforge");
        }
    }

    @Test
    void provisionAppRoleAndDatabase_whenRunAgain_doesNotFailAndDatabaseStaysUsable() {
        assertThatCode(() -> localDatabaseConfig.provisionAppRoleAndDatabase(embeddedPostgres))
                .doesNotThrowAnyException();

        assertThatCode(() -> {
            try (Connection connection = embeddedPostgres.getDatabase("cloudforge", "cloudforge").getConnection()) {
                connection.createStatement().execute("SELECT 1");
            }
        }).doesNotThrowAnyException();
    }
}
