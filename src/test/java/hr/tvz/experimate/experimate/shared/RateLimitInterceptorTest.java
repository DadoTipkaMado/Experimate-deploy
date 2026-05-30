package hr.tvz.experimate.experimate.shared;

import hr.tvz.experimate.experimate.domain.user.Role;
import hr.tvz.experimate.experimate.security.AppUserDetails;
import hr.tvz.experimate.experimate.shared.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock private RateLimiterService rateLimiterService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private RateLimitInterceptor interceptor;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preHandle_whenAuthenticatedUserExceedsBaseline_throwsRateLimitException() {
        AppUserDetails principal = new AppUserDetails(42, "testuser", null, Role.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        doThrow(new RateLimitException("Rate limit exceeded. Please slow down and try again later."))
                .when(rateLimiterService).consume(RateLimitOperation.API_BASELINE, 42);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(RateLimitException.class);
    }

    @Test
    void preHandle_whenAnonymousUserExceedsBaseline_throwsRateLimitException() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");

        doThrow(new RateLimitException("Rate limit exceeded. Please slow down and try again later."))
                .when(rateLimiterService).consume(RateLimitOperation.API_BASELINE_ANON, "1.2.3.4");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(RateLimitException.class);
    }

    @Test
    void preHandle_whenAnonymousUserWithForwardedIpExceedsBaseline_throwsRateLimitException() {
        // X-Forwarded-For may contain a comma-separated chain of IPs when passing multiple proxies.
        // Only the first (original client) IP is used as the rate-limit key.
        when(request.getHeader("X-Forwarded-For")).thenReturn("9.8.7.6, 10.0.0.1");

        doThrow(new RateLimitException("Rate limit exceeded. Please slow down and try again later."))
                .when(rateLimiterService).consume(RateLimitOperation.API_BASELINE_ANON, "9.8.7.6");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(RateLimitException.class);
    }
}
