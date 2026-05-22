package hr.tvz.experimate.experimate.shared;

import io.github.bucket4j.Bandwidth;

import java.time.Duration;
import java.util.List;

public enum RateLimitOperation {
    AI_SEARCH(Bandwidth.builder()
            .capacity(10)
            .refillIntervally(10, Duration.ofHours(1))
            .build()
    ),
    AI_EXPLAIN(Bandwidth.builder()
            .capacity(10)
            .refillIntervally(10, Duration.ofMinutes(5))
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
