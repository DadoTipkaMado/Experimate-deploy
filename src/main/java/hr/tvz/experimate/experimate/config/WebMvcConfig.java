package hr.tvz.experimate.experimate.config;

import hr.tvz.experimate.experimate.shared.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for cross-cutting request interceptors.
 *
 * <p>Registers {@link RateLimitInterceptor} on all {@code /api/**} paths, excluding
 * public image-serving endpoints. Those GET endpoints are {@code permitAll} in
 * {@link hr.tvz.experimate.experimate.config.SecurityConfig} and serve static assets —
 * applying the anonymous baseline quota there would cause page loads (with many images)
 * to exhaust the per-IP bucket immediately.
 *
 * <p>The PWA service worker ({@code /sw.js}) is served by Spring Boot's default static
 * resource handler from {@code classpath:/static/}; no explicit handler is registered here.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/profile-photo/**",
                        "/api/partner-pins/logo/**",
                        "/api/promoted-ads/image/**"
                );
    }
}
