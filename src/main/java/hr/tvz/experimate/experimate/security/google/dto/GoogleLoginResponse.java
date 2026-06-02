package hr.tvz.experimate.experimate.security.google.dto;

/**
 * HTTP response for {@code POST /api/auth/google}.
 *
 * <p>Two variants, discriminated by the {@code status} field:
 * <ul>
 *   <li>{@link Authenticated} — user found; carries an access token.</li>
 *   <li>{@link RegistrationRequired} — no existing account; carries Google-verified profile
 *       data for the frontend to pre-fill the registration form.</li>
 * </ul>
 *
 * <p>Use the static factory methods {@link #authenticated} and {@link #registrationRequired}
 * to construct instances — they set {@code status} to the correct value.
 */
public sealed interface GoogleLoginResponse permits GoogleLoginResponse.Authenticated, GoogleLoginResponse.RegistrationRequired {

    record Authenticated(String status, String accessToken) implements GoogleLoginResponse {}

    record RegistrationRequired(String status, GoogleProfile googleProfile) implements GoogleLoginResponse {}

    /**
     * Google-verified user data for pre-filling the registration form.
     */
    record GoogleProfile(String firstName, String lastName, String email) {}

    static Authenticated authenticated(String accessToken) {
        return new Authenticated("LOGGED_IN", accessToken);
    }

    static RegistrationRequired registrationRequired(GoogleProfile googleProfile) {
        return new RegistrationRequired("REGISTRATION_REQUIRED", googleProfile);
    }
}
