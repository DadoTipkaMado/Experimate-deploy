package hr.tvz.experimate.experimate.security.google;

import hr.tvz.experimate.experimate.security.google.dto.GoogleLoginResponse;
import hr.tvz.experimate.experimate.shared.response.TokenResponse;

/**
 * Internal result of {@link GoogleAuthService#loginOrInitiateRegistration}.
 * Exactly one of {@code tokens} or {@code googleProfile} is non-null:
 * <ul>
 *   <li>{@code tokens} set → user exists, ready to log in.</li>
 *   <li>{@code googleProfile} set → no existing user, registration required.</li>
 * </ul>
 */
public record GoogleAuthResult(TokenResponse tokens, GoogleLoginResponse.GoogleProfile googleProfile) {}
