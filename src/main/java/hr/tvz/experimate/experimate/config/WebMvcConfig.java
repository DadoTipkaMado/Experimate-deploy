package hr.tvz.experimate.experimate.config;

import hr.tvz.experimate.experimate.shared.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for PWA support and cross-cutting request interceptors.
 *
 * <p>Registers {@link RateLimitInterceptor} on all {@code /api/**} paths, excluding
 * public image-serving endpoints. Those GET endpoints are {@code permitAll} in
 * {@link hr.tvz.experimate.experimate.config.SecurityConfig} and serve static assets —
 * applying the anonymous baseline quota there would cause page loads (with many images)
 * to exhaust the per-IP bucket immediately.
 *
 * <p>Also serves the service worker ({@code /sw.js}) with {@code Cache-Control: no-cache}
 * so PWA updates propagate to clients without waiting for browser cache expiry.
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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/sw.js")
                .addResourceLocations("classpath:/static/sw.js")
                .setCacheControl(CacheControl.noCache());
    }
}
