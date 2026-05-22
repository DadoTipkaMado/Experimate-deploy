package hr.tvz.experimate.experimate.shared;

import hr.tvz.experimate.experimate.shared.exception.RateLimitException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.local.LocalBucketBuilder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void consume(RateLimitOperation operation, Integer userId) {
        String key = operation.name() + ":" + userId;
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(operation));

        if (!bucket.tryConsume(1))
            throw new RateLimitException("Rate limit exceeded. Please slow down and try again later.");
    }

    /**
     * Rate-limits an operation keyed by an arbitrary string (e.g. email address).
     * Used for unauthenticated endpoints where no user ID is available.
     *
     * @param operation the operation whose bucket limits apply
     * @param key       the rate-limit key (e.g. normalized email address)
     * @throws RateLimitException if the bucket for this key is exhausted
     */
    public void consume(RateLimitOperation operation, String key) {
        String bucketKey = operation.name() + ":" + key;
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(operation));

        if (!bucket.tryConsume(1))
            throw new RateLimitException("Rate limit exceeded. Please slow down and try again later.");
    }

    private Bucket createBucket(RateLimitOperation operation) {
        LocalBucketBuilder builder = Bucket.builder();
        operation.limits.forEach(builder::addLimit);
        return builder.build();
    }
}
