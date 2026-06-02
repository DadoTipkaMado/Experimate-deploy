package hr.tvz.experimate.experimate.domain.rating;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import hr.tvz.experimate.experimate.domain.rating.dto.CreateRatingDto;
import hr.tvz.experimate.experimate.domain.rating.response.RatingResponse;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RatingIT extends AbstractIntegrationTest {

    private static final String RATING_URL = "/api/rating";
    private static final String REVIEW     = "A".repeat(20);

    @Autowired private RatingRepo ratingRepo;
    @Autowired private UserRepo userRepo;

    private record RatingArrange(String hostJwt, String guestJwt, Integer hostId) {}

    /**
     * Creates a CLOSED reservation: listing → booking request → accept → both check-in → end tour.
     * Returns JWTs for both parties and the host's user ID (needed as ratedId in {@link CreateRatingDto}).
     */
    private RatingArrange arrangeClosedTour() {
        Map<String, String> hostTokens  = loginAndGetTokens("host");
        Map<String, String> guestTokens = loginAndGetTokens("guest");
        Integer reservationId = createReservation(hostTokens.get("accessToken"), guestTokens.get("accessToken"));
        checkIn(guestTokens.get("accessToken"), reservationId);
        checkIn(hostTokens.get("accessToken"), reservationId);
        endTour(guestTokens.get("accessToken"), reservationId);
        Integer hostId = userRepo.findByUsername("host").get().getId();
        return new RatingArrange(hostTokens.get("accessToken"), guestTokens.get("accessToken"), hostId);
    }

    // ── POST /api/rating ──────────────────────────────────────────────────────

    @Test
    void createRating_byGuestAfterClosedTour_returns201AndPersistsInDatabase() {
        RatingArrange arrange = arrangeClosedTour();

        ResponseEntity<RatingResponse> response = restTemplate.exchange(
                RATING_URL, HttpMethod.POST,
                new HttpEntity<>(new CreateRatingDto(arrange.hostId(), 5, REVIEW), bearerHeaders(arrange.guestJwt())),
                RatingResponse.class
        );

        RatingResponse body = response.getBody();
        Optional<Rating> savedRating = ratingRepo.findById(body.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(body.id()).isNotNull();
        assertThat(body.score()).isEqualTo(5);
        assertThat(body.review()).isEqualTo(REVIEW);
        assertThat(savedRating).isPresent();
        assertThat(savedRating.get().getScore()).isEqualTo(5);
        assertThat(savedRating.get().getReview()).isEqualTo(REVIEW);
    }

    @Test
    void createRating_afterTwoGuestsRateHost_recalculatesHostRatingToAverage() {
        RatingArrange arrange = arrangeClosedTour();
        Map<String, String> guest2Tokens = loginAndGetTokens("guest2");
        Integer reservation2Id = createReservation(arrange.hostJwt(), guest2Tokens.get("accessToken"));
        checkIn(guest2Tokens.get("accessToken"), reservation2Id);
        checkIn(arrange.hostJwt(), reservation2Id);
        endTour(guest2Tokens.get("accessToken"), reservation2Id);

        restTemplate.exchange(
                RATING_URL, HttpMethod.POST,
                new HttpEntity<>(new CreateRatingDto(arrange.hostId(), 5, REVIEW), bearerHeaders(arrange.guestJwt())),
                RatingResponse.class
        );
        restTemplate.exchange(
                RATING_URL, HttpMethod.POST,
                new HttpEntity<>(new CreateRatingDto(arrange.hostId(), 3, REVIEW), bearerHeaders(guest2Tokens.get("accessToken"))),
                RatingResponse.class
        );

        Optional<User> host = userRepo.findByUsername("host");

        assertThat(host).isPresent();
        assertThat(host.get().getRating()).isEqualTo(4.0);
    }
}
