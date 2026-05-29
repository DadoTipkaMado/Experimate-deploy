package hr.tvz.experimate.experimate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC override-i za PWA potrebe. Trenutno servira service worker
 * ({@code /sw.js}) s {@code Cache-Control: no-cache} kako bi se nove verzije
 * SW-a propagirale userima odmah, bez čekanja isteka browser cache-a.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/sw.js")
                .addResourceLocations("classpath:/static/sw.js")
                .setCacheControl(CacheControl.noCache());
    }
}
