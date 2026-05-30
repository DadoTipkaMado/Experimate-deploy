package hr.tvz.experimate.experimate.shared;

import io.github.bucket4j.Bandwidth;

import java.time.Duration;
import java.util.List;

public enum RateLimitOperation {
    // Coarse baseline limits applied to every /api/** request by RateLimitInterceptor.
    // API_BASELINE is keyed per authenticated user, API_BASELINE_ANON per client IP.
    API_BASELINE(Bandwidth.builder()
            .capacity(100)
            .refillIntervally(100, Duration.ofMinutes(1))
            .build()
    ),
    API_BASELINE_ANON(Bandwidth.builder()
            .capacity(10)
            .refillIntervally(10, Duration.ofMinutes(1))
            .build()
    ),
    // Cost-driven AI limits. Reachable only by PREMIUM_USER (enforced via @PreAuthorize),
    // so a single per-user quota is sufficient.
    AI_SEARCH(Bandwidth.builder()
            .capacity(50)
            .refillIntervally(50, Duration.ofHours(1))
            .build()
    ),
    AI_EXPLAIN(Bandwidth.builder()
            .capacity(40)
            .refillIntervally(40, Duration.ofMinutes(5))
            .build()
    ),
    EMAIL_RESEND(Bandwidth.builder()
            .capacity(5)
            .refillIntervally(5, Duration.ofHours(1))
            .build()
    ),
    PASSWORD_RESET(Bandwidth.builder()
            .capacity(5)
            .refillIntervally(5, Duration.ofHours(1))
            .build()
    );

    final List<Bandwidth> limits;

    RateLimitOperation(Bandwidth... limits){
        this.limits = List.of(limits);
    }
}
