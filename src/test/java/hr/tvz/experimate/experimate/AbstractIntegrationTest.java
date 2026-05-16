package hr.tvz.experimate.experimate;

import hr.tvz.experimate.experimate.domain.user.UserService;
import hr.tvz.experimate.experimate.domain.user.dto.CreateUserDto;
import hr.tvz.experimate.experimate.security.AuthResponse;
import hr.tvz.experimate.experimate.security.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

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
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    protected ChatClient chatClient;

    private static final String LOGIN_URL = "/api/auth/login";
    private static final String LOGIN_PASS = "123123123123";

    @Autowired
    UserService userService;

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

    private void createUser(String username) {
        long idNum = Math.abs((long) username.hashCode());
        userService.createUser(new CreateUserDto(
                "Test", "User", LocalDate.of(2000, 1, 1),
                String.format("%020d", idNum), username, LOGIN_PASS, null
        ));
    }

    protected Map<String, String> loginAndGetTokens(String username) {
        createUser(username);
        Map<String, String> tokens = new HashMap<>();
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                LOGIN_URL,
                new LoginRequest(username, LOGIN_PASS),
                AuthResponse.class
        );
        String setCookieHeader = response.getHeaders().getFirst("Set-Cookie");
        // "refresh_token=<value>; Path=...; HttpOnly; Max-Age=..." → extract only the value
        tokens.put("refreshToken", setCookieHeader.split(";")[0].split("=", 2)[1]);
        tokens.put("accessToken", response.getBody().jwt());

        return tokens;
    }
}
