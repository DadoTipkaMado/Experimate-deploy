package hr.tvz.experimate.experimate.model.shared.event;

public record RatingRecalculatedEvent(Integer userId,
                                      Double ratingScore) {
}
