package hr.tvz.experimate.experimate.shared.event;

public record RatingRecalculatedEvent(Integer userId,
                                      Double ratingScore) {
}
