package hr.tvz.experimate.experimate;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base class for every integration test in the project.
 * <p>
 * Provides:
 * <ul>
 *   <li>a real PostgreSQL instance via Testcontainers (shared across the test class),</li>
 *   <li>a {@link TestRestTemplate} pre-wired to the random server port,</li>
 *   <li>a Mockito-replaced {@link ChatClient} so Spring AI calls never hit the network,</li>
 *   <li>a {@code @BeforeEach} hook that truncates every table so each test starts clean.</li>
 * </ul>
 * Concrete integration tests must end in {@code IT} so Maven Failsafe picks them up.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    protected ChatClient chatClient;

    /**
     * Truncates every user table (everything except {@code flyway_schema_history}) before each test,
     * resetting auto-generated IDs and cascading through foreign keys.
     * Keeps tests independent of execution order.
     */
    @BeforeEach
    void truncateAllTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                "DO $$ DECLARE r RECORD; BEGIN " +
                "  FOR r IN SELECT tablename FROM pg_tables " +
                "           WHERE schemaname = 'public' " +
                "             AND tablename <> 'flyway_schema_history' LOOP " +
                "    EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || " +
                "            ' RESTART IDENTITY CASCADE'; " +
                "  END LOOP; " +
                "END $$;"
            );
        }
    }
}
