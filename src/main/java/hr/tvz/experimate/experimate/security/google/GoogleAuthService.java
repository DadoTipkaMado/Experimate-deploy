package hr.tvz.experimate.experimate.security.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import hr.tvz.experimate.experimate.domain.refresh_token.RefreshTokenService;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.security.JwtService;
import hr.tvz.experimate.experimate.security.google.dto.GoogleLoginRequest;
import hr.tvz.experimate.experimate.security.google.dto.GoogleLoginResponse;
import hr.tvz.experimate.experimate.security.google.dto.GoogleRegistrationRequest;
import hr.tvz.experimate.experimate.security.google.exception.IncompleteGoogleProfileException;
import hr.tvz.experimate.experimate.security.google.exception.InvalidGoogleTokenException;
import hr.tvz.experimate.experimate.security.google.exception.UnverifiedGoogleEmailException;
import hr.tvz.experimate.experimate.shared.event.GoogleUserCreationEvent;
import hr.tvz.experimate.experimate.shared.response.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    private final GoogleIdTokenVerifier verifier;
    private final UserRepo userRepo;
    private final ApplicationEventPublisher publisher;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public GoogleAuthService(GoogleIdTokenVerifier verifier,
                             UserRepo userRepo,
                             ApplicationEventPublisher publisher,
                             JwtService jwtService,
                             RefreshTokenService refreshTokenService) {
        this.verifier = verifier;
        this.userRepo = userRepo;
        this.publisher = publisher;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Verifies the Google ID token, then either logs in an existing user or signals that
     * registration is needed.
     *
     * <p>Lookup order:
     * <ol>
     *   <li>Match by {@code googleSub} — stable even if the user later changes their email.</li>
     *   <li>Match by verified email — links an existing password-registered account to Google.</li>
     *   <li>No match → return {@code REGISTRATION_REQUIRED} with Google profile data as prefill.</li>
     * </ol>
     *
     * @param req the incoming request carrying the Google ID token
     * @return {@link GoogleAuthResult} with tokens set if logged in, or googleProfile set if registration is needed
     * @throws InvalidGoogleTokenException    if the token is invalid or expired
     * @throws UnverifiedGoogleEmailException if {@code email_verified} is false in the token
     */
    @Transactional
    public GoogleAuthResult loginOrInitiateRegistration(GoogleLoginRequest req) {
        GoogleIdToken.Payload payload = verifyToken(req.idToken());
        GoogleProfileClaims claims = extractClaims(payload);

        String googleSub = claims.googleSub();
        String email     = claims.email();
        String firstName = claims.firstName();
        String lastName  = claims.lastName();

        // Match by stable Google sub
        Optional<User> byGoogleSub = userRepo.findByGoogleSub(googleSub);
        if (byGoogleSub.isPresent()) {
            log.info("Google login: matched by googleSub for user {}", byGoogleSub.get().getUsername());
            return new GoogleAuthResult(buildTokenResponse(byGoogleSub.get()), null);
        }

        // Match by verified email — link google sub to existing account
        Optional<User> byEmail = userRepo.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            user.setGoogleSub(googleSub);
            user.setEmailVerified(true);
            userRepo.save(user);
            log.info("Google login: linked googleSub to existing user {} via email", user.getUsername());
            return new GoogleAuthResult(buildTokenResponse(user), null);
        }

        log.info("Google login: no existing user found, registration required for email {}", email);
        return new GoogleAuthResult(null, new GoogleLoginResponse.GoogleProfile(firstName, lastName, email));
    }

    /**
     * Completes Google OAuth2 registration by re-verifying the original ID token and creating
     * a new user account with the supplied credentials and Google-verified profile data.
     *
     * <p>Re-verifying the token (instead of trusting the frontend) proves that the same Google
     * user who triggered the first call is completing the registration.
     *
     * @param req registration data including the original ID token and user-supplied fields
     * @return {@link TokenResponse} with access and refresh tokens for the newly created user
     * @throws InvalidGoogleTokenException    if the ID token is no longer valid
     * @throws UnverifiedGoogleEmailException if {@code email_verified} is false in the token
     */
    @Transactional
    public TokenResponse completeRegistration(GoogleRegistrationRequest req) {
        GoogleIdToken.Payload payload = verifyToken(req.idToken());
        GoogleProfileClaims claims = extractClaims(payload);

        String googleSub = claims.googleSub();
        String email     = claims.email();
        String firstName = claims.firstName();
        String lastName  = claims.lastName();

        // Handle race condition: user may have been created between the two calls
        Optional<User> existing = userRepo.findByGoogleSub(googleSub);
        if (existing.isPresent()) {
            log.info("Google complete-registration: user already exists (race condition), logging in");
            return buildTokenResponse(existing.get());
        }

        // @EventListener fires synchronously — user is created before publishEvent returns
        publisher.publishEvent(new GoogleUserCreationEvent(
                firstName, lastName, req.dateOfBirth(), req.idNumber(),
                email, req.username(), req.password(), googleSub
        ));

        User user = userRepo.findByGoogleSub(googleSub).orElseThrow();
        log.info("Google registration completed for user {}", user.getUsername());
        return buildTokenResponse(user);
    }

    /**
     * Verifies the Google ID token signature and audience, and asserts that
     * the email in the payload is verified by Google.
     *
     * @param idToken the raw Google ID token string from the client
     * @return the verified token payload
     * @throws InvalidGoogleTokenException    if the token is null, expired, or has an invalid signature
     * @throws UnverifiedGoogleEmailException if {@code email_verified} is false in the payload
     */
    private GoogleIdToken.Payload verifyToken(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                log.warn("Google ID token verification returned null — invalid or expired");
                throw new InvalidGoogleTokenException();
            }
            // email_verified can be false for Google Workspace accounts with custom domains
            if (!Boolean.TRUE.equals(token.getPayload().getEmailVerified())) {
                log.warn("Google ID token has unverified email");
                throw new UnverifiedGoogleEmailException();
            }
            return token.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            log.warn("Google ID token verification failed: {}", e.getMessage());
            throw new InvalidGoogleTokenException();
        }
    }

    private record GoogleProfileClaims(String googleSub, String email, String firstName, String lastName) {}

    /**
     * Extracts and validates required profile claims from a verified Google ID token payload.
     * Throws {@link IncompleteGoogleProfileException} if any required claim is absent,
     * which happens when the frontend omits the {@code email} scope or the account lacks a name.
     */
    private GoogleProfileClaims extractClaims(GoogleIdToken.Payload payload) {
        String email     = (String) payload.get("email");
        String firstName = (String) payload.get("given_name");
        String lastName  = (String) payload.get("family_name");
        // email scope not requested, or Google Workspace account without required fields
        if (email == null || firstName == null || lastName == null) {
            log.warn("Google ID token payload missing required claims (email/given_name/family_name)");
            throw new IncompleteGoogleProfileException();
        }
        return new GoogleProfileClaims(payload.getSubject(), email.toLowerCase(), firstName, lastName);
    }

    private TokenResponse buildTokenResponse(User user) {
        return new TokenResponse(
                jwtService.generateToken(user.getUsername()),
                refreshTokenService.createOrUpdateRefreshToken(user)
        );
    }
}
