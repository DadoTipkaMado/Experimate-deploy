package hr.tvz.experimate.experimate.shared;

import hr.tvz.experimate.experimate.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Baseline rate-limit guard applied to every {@code /api/**} request.
 *
 * <p>This is the first defence layer against DoS and scraping — it enforces a broad
 * request-per-minute quota before business logic runs. Fine-grained per-operation
 * limits (AI calls, email sends) are still enforced explicitly in the service layer
 * via {@link RateLimiterService#consume(RateLimitOperation, Integer)}.
 *
 * <p>Two buckets are used:
 * <ul>
 *   <li>{@link RateLimitOperation#API_BASELINE} — for authenticated users, keyed by user ID.</li>
 *   <li>{@link RateLimitOperation#API_BASELINE_ANON} — for unauthenticated requests, keyed by
 *       client IP. {@code X-Forwarded-For} is read first to handle reverse-proxy deployments.</li>
 * </ul>
 *
 * <p>Registered in {@link hr.tvz.experimate.experimate.config.WebMvcConfig} with exclusions
 * for public image-serving paths so that loading profile photos and partner logos does not
 * count against the anonymous quota.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AppUserDetails principal) {
            rateLimiterService.consume(RateLimitOperation.API_BASELINE, principal.getId());
        } else {
            rateLimiterService.consume(RateLimitOperation.API_BASELINE_ANON, resolveClientIp(request));
        }

        return true;
    }

    /**
     * Resolves the real client IP, preferring the first address in {@code X-Forwarded-For}
     * when the app runs behind a reverse proxy.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
