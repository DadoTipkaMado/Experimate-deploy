package hr.tvz.experimate.experimate.domain.match.response;

import java.time.LocalDateTime;

/**
 * Summary of a candidate's active tour listing, included in a {@link MatchResponse}.
 *
 * @param id                 listing ID
 * @param city               city where the tour takes place
 * @param meetingDate        proposed meeting date and time
 * @param descriptionSnippet first 200 characters of the tour description
 */
public record ActiveListingInfo(
        Integer id,
        String city,
        LocalDateTime meetingDate,
        String descriptionSnippet
) {}
