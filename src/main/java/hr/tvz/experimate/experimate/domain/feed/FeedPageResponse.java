package hr.tvz.experimate.experimate.domain.feed;

import java.util.List;

/**
 * API response wrapper for the interleaved feed of tour listings and promoted ads.
 *
 * <p>Declared as a non-parameterised record with {@code List<FeedItem>} as a concrete
 * component type. Returning {@code Page<FeedItem>} directly does not work because
 * Spring Data's {@code PageModule} serialises {@code PageImpl} content elements as
 * plain beans, never calling the polymorphic type writer required by
 * {@link com.fasterxml.jackson.annotation.JsonTypeInfo} on {@link FeedItem}.
 */
public record FeedPageResponse(
        List<FeedItem> content,
        int number,
        int size,
        long totalElements
) {}
