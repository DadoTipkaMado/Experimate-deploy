package hr.tvz.experimate.experimate;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import hr.tvz.experimate.experimate.push.PushGateway;
import hr.tvz.experimate.experimate.domain.booking_request.dto.CreateBookingRequestDto;
import hr.tvz.experimate.experimate.domain.booking_request.response.BookingRequestResponse;
import hr.tvz.experimate.experimate.domain.partner.ApplyPartnerRequest;
import hr.tvz.experimate.experimate.domain.partner_event.CreatePartnerEventRequest;
import hr.tvz.experimate.experimate.domain.partner_event.PartnerEventResponse;
import hr.tvz.experimate.experimate.domain.partner_pin.CreatePartnerPinRequest;
import hr.tvz.experimate.experimate.domain.partner_pin.PartnerPinResponse;
import hr.tvz.experimate.experimate.domain.promoted_ad.CreatePromotedAdRequest;
import hr.tvz.experimate.experimate.domain.promoted_ad.PromotedAdResponse;
import hr.tvz.experimate.experimate.domain.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.domain.reservation.response.EndTourResponse;
import hr.tvz.experimate.experimate.domain.tour_listing.dto.CreateTourListingDto;
import hr.tvz.experimate.experimate.domain.tour_listing.response.TourListingResponse;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.domain.user.UserService;
import hr.tvz.experimate.experimate.domain.user.dto.CreateUserDto;
import hr.tvz.experimate.experimate.security.AuthResponse;
import hr.tvz.experimate.experimate.security.LoginRequest;
import hr.tvz.experimate.experimate.config.TestAsyncConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
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
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
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
 *   <li>a {@code @BeforeEach} hook that truncates every table so each test starts clean,</li>
 *   <li>shared setup helpers for the reservation flow used across integration test classes.</li>
 * </ul>
 * Concrete integration tests must end in {@code IT} so Maven Failsafe picks them up.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Import(TestAsyncConfig.class)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    protected static final GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP);

    static {
        POSTGRES.start();
        greenMail.start();
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    protected ReservationRepo reservationRepo;

    @MockitoBean
    protected ChatClient chatClient;

    /**
     * Mocked so no actual HTTP calls are made to push services during tests.
     * Returns 0 (not 410) by default, so the 410-cleanup path in PushNotificationService
     * is never triggered during tests.
     */
    @MockitoBean
    protected PushGateway pushGateway;

    protected static final String LISTING_URL     = "/api/tour-listing";
    protected static final String BOOKING_URL     = "/api/booking-request";
    protected static final String RESERVATION_URL = "/api/reservation";
    protected static final String DESCRIPTION     = "A".repeat(20);

    private static final String LOGIN_URL  = "/api/auth/login";
    private static final String LOGIN_PASS = "123123123123";

    @Autowired
    UserService userService;

    @Autowired
    private UserRepo userRepo;

    /**
     * Truncates every user table (everything except {@code flyway_schema_history}) before each test,
     * resetting auto-generated IDs and cascading through foreign keys.
     * Keeps tests independent of execution order.
     */
    @BeforeEach
    void truncateAllTables() throws SQLException {
        greenMail.reset();
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
                String.format("%020d", idNum), username + "@test.com", username, LOGIN_PASS, null
        ));
        // bypass the email verification gate so test helpers can log in immediately
        userRepo.findByUsername(username).ifPresent(u -> {
            u.setEmailVerified(true);
            userRepo.save(u);
        });
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

    /**
     * Builds HTTP headers with a Bearer token and JSON content type.
     */
    protected HttpHeaders bearerHeaders(String jwt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Calls {@code POST /api/partner/apply} on behalf of an already-authenticated user,
     * promoting them to the PARTNER role. Reusable across all IT classes that need a partner account.
     */
    protected void applyAsPartner(String jwt) {
        restTemplate.exchange(
                "/api/partner/apply", HttpMethod.POST,
                new HttpEntity<>(new ApplyPartnerRequest("Test Corp", "test@corp.com", null), bearerHeaders(jwt)),
                Void.class
        );
    }

    /**
     * Creates a partner pin at the given coordinates and returns its ID.
     * Requires the caller to already hold a PARTNER role JWT.
     */
    protected Integer createPin(String jwt, double lat, double lng) {
        return restTemplate.exchange(
                "/api/partner-pins", HttpMethod.POST,
                new HttpEntity<>(new CreatePartnerPinRequest("Test Pin", null, lat, lng), bearerHeaders(jwt)),
                PartnerPinResponse.class
        ).getBody().id();
    }

    /**
     * Creates a partner event on the given pin and returns its ID.
     * {@code start} must be in the future to satisfy {@code @Future} validation.
     */
    protected Integer createEvent(String jwt, Integer pinId, LocalDateTime start, String title) {
        return restTemplate.exchange(
                "/api/partner-pins/" + pinId + "/events", HttpMethod.POST,
                new HttpEntity<>(new CreatePartnerEventRequest(title, null, null, start, start.plusHours(2)), bearerHeaders(jwt)),
                PartnerEventResponse.class
        ).getBody().id();
    }

    /**
     * Creates a promoted ad (always active, no image) and returns its ID.
     * Requires the caller to already hold a PARTNER role JWT.
     */
    protected Integer createAd(String jwt, String title) {
        return restTemplate.exchange(
                "/api/promoted-ads", HttpMethod.POST,
                new HttpEntity<>(new CreatePromotedAdRequest(title, null, null, null, null), bearerHeaders(jwt)),
                PromotedAdResponse.class
        ).getBody().id();
    }

    /**
     * Creates a tour listing with meetingDate {@code amount} {@code unit} from now.
     * Use {@code 25, ChronoUnit.MINUTES} for check-in tests (window opens 30 min before meeting).
     */
    protected Integer createListing(String hostJwt, long amount, TemporalUnit unit) {
        return createListing(hostJwt, amount, unit, 2);
    }

    protected Integer createListing(String hostJwt, long amount, TemporalUnit unit, int maxGuests) {
        CreateTourListingDto dto = new CreateTourListingDto(
                "Zagreb", 15.966568, 45.815399,
                LocalDateTime.now().plus(amount, unit),
                DESCRIPTION.repeat(20),
                maxGuests
        );
        return restTemplate.exchange(
                LISTING_URL, HttpMethod.POST,
                new HttpEntity<>(dto, bearerHeaders(hostJwt)),
                TourListingResponse.class
        ).getBody().id();
    }

    /**
     * Runs the full flow: create listing → send booking request → host accepts.
     * Returns the ID of the resulting reservation.
     */
    protected Integer createReservation(String hostJwt, String guestJwt) {
        Integer listingId = createListing(hostJwt, 25, java.time.temporal.ChronoUnit.MINUTES);
        Integer requestId = restTemplate.exchange(
                BOOKING_URL, HttpMethod.POST,
                new HttpEntity<>(new CreateBookingRequestDto(listingId), bearerHeaders(guestJwt)),
                BookingRequestResponse.class
        ).getBody().id();
        restTemplate.exchange(
                BOOKING_URL + "/accept/" + requestId, HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(hostJwt)), Void.class
        );
        return reservationRepo.findByTourListing_Id(listingId).orElseThrow().getId();
    }

    /**
     * Sends a check-in request for the given reservation as the authenticated user.
     */
    protected void checkIn(String jwt, Integer reservationId) {
        restTemplate.exchange(
                RESERVATION_URL + "/check-in/" + reservationId, HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(jwt)), Void.class
        );
    }

    /**
     * Ends the tour for the given reservation as the authenticated user.
     */
    protected ResponseEntity<EndTourResponse> endTour(String jwt, Integer reservationId) {
        return restTemplate.exchange(
                RESERVATION_URL + "/end-tour/" + reservationId, HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(jwt)),
                EndTourResponse.class
        );
    }
}
