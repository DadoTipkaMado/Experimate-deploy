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

    private Bucket createBucket(RateLimitOperation operation) {
        LocalBucketBuilder builder = Bucket.builder();
        operation.limits.forEach(builder::addLimit);
        return builder.build();
    }
}
