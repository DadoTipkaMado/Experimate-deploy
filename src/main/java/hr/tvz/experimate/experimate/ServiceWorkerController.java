package hr.tvz.experimate.experimate;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the PWA service worker script ({@code /sw.js}) with an explicit
 * {@code Cache-Control: no-cache} header.
 *
 * <p>Spring Boot's default static resource handler would also serve this file from
 * {@code classpath:/static/}, but it applies the global {@code spring.web.resources.cache.period}
 * (24h) to it. With a long-lived cache header the browser may keep an old worker around,
 * delaying PWA updates. Serving it through this controller forces revalidation on every
 * request so a bumped {@code CACHE} version in {@code sw.js} reaches clients immediately.
 *
 * <p>A dedicated controller is used rather than a single-file {@code ResourceHandlerRegistry}
 * mapping: an exact resource-handler pattern such as {@code /sw.js} resolves to an empty
 * path within its location and ends up pointing at the directory, which returns 404.
 */
@RestController
public class ServiceWorkerController {

    private static final Resource SERVICE_WORKER = new ClassPathResource("static/sw.js");

    /**
     * Returns the service worker script with {@code no-cache} so the browser always
     * revalidates it against the server.
     *
     * @return the {@code sw.js} resource, served as JavaScript with no-cache headers
     */
    @GetMapping(value = "/sw.js", produces = "text/javascript")
    public ResponseEntity<Resource> serviceWorker() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.valueOf("text/javascript"))
                .body(SERVICE_WORKER);
    }
}
