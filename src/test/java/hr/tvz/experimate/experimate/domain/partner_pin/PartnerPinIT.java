package hr.tvz.experimate.experimate.domain.partner_pin;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerPinIT extends AbstractIntegrationTest {

    private static final String PIN_URL = "/api/partner-pins";

    // ──────────────── createPin ────────────────

    @Test
    void createPin_returns201WithPinData() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);

        ResponseEntity<PartnerPinResponse> response = restTemplate.exchange(
                PIN_URL, HttpMethod.POST,
                new HttpEntity<>(new CreatePartnerPinRequest("My Venue", null, 45.0, 16.0), bearerHeaders(jwt)),
                PartnerPinResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().name()).isEqualTo("My Venue");
        assertThat(response.getBody().latitude()).isEqualTo(45.0);
        assertThat(response.getBody().longitude()).isEqualTo(16.0);
    }

    // ──────────────── getMyPins ────────────────

    @Test
    void getMyPins_returnsOnlyOwnPins() {
        String jwt1 = loginAndGetTokens("partner1").get("accessToken");
        String jwt2 = loginAndGetTokens("partner2").get("accessToken");
        applyAsPartner(jwt1);
        applyAsPartner(jwt2);
        createPin(jwt1, 45.0, 16.0);
        createPin(jwt2, 46.0, 17.0);

        ResponseEntity<List<PartnerPinResponse>> response = restTemplate.exchange(
                PIN_URL + "/mine", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt1)),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).latitude()).isEqualTo(45.0);
    }

    @Test
    void updatePin_returns403ForNonOwner() {
        String ownerJwt  = loginAndGetTokens("owner").get("accessToken");
        String partnerJwt = loginAndGetTokens("otherpartner").get("accessToken");
        String regularJwt = loginAndGetTokens("regular").get("accessToken");
        applyAsPartner(ownerJwt);
        applyAsPartner(partnerJwt);
        Integer pinId = createPin(ownerJwt, 45.0, 16.0);

        UpdatePartnerPinRequest req = new UpdatePartnerPinRequest("Hacked", null, null, null, null);

        // Non-partner: fails role check
        assertThat(restTemplate.exchange(PIN_URL + "/" + pinId, HttpMethod.PUT,
                new HttpEntity<>(req, bearerHeaders(regularJwt)), Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Valid PARTNER but not the owner: fails ownership check
        assertThat(restTemplate.exchange(PIN_URL + "/" + pinId, HttpMethod.PUT,
                new HttpEntity<>(req, bearerHeaders(partnerJwt)), Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getMyPins_returns403ForNonPartner() {
        String jwt = loginAndGetTokens("regularuser").get("accessToken");

        assertThat(restTemplate.exchange(PIN_URL + "/mine", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt)), Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ──────────────── getAllActivePins ────────────────

    @Test
    void getAllActivePins_returnsPinsFromAllPartners() {
        String jwt1 = loginAndGetTokens("partner1").get("accessToken");
        String jwt2 = loginAndGetTokens("partner2").get("accessToken");
        applyAsPartner(jwt1);
        applyAsPartner(jwt2);
        createPin(jwt1, 45.0, 16.0);
        createPin(jwt2, 46.0, 17.0);

        String anyJwt = loginAndGetTokens("viewer").get("accessToken");
        ResponseEntity<List<PartnerPinResponse>> response = restTemplate.exchange(
                PIN_URL, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(anyJwt)),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    // ──────────────── updatePin ────────────────

    @Test
    void updatePin_returns200WithUpdatedName() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId = createPin(jwt, 45.0, 16.0);

        ResponseEntity<PartnerPinResponse> response = restTemplate.exchange(
                PIN_URL + "/" + pinId, HttpMethod.PUT,
                new HttpEntity<>(new UpdatePartnerPinRequest("Updated Venue", null, null, null, null), bearerHeaders(jwt)),
                PartnerPinResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("Updated Venue");
    }

    // ──────────────── deletePin ────────────────

    @Test
    void deletePin_returns204AndPinNoLongerVisible() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId = createPin(jwt, 45.0, 16.0);

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                PIN_URL + "/" + pinId, HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(jwt)),
                Void.class
        );

        ResponseEntity<List<PartnerPinResponse>> listResponse = restTemplate.exchange(
                PIN_URL, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt)),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(listResponse.getBody()).isEmpty();
    }
}
