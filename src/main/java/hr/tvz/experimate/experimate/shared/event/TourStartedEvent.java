package hr.tvz.experimate.experimate.shared.event;

/**
 * Published when a tour transitions to started — either automatically (all participants checked in)
 * or manually (host killswitch). Handled by {@code ReservationService} to activate ready reservations.
 */
public record TourStartedEvent(Integer listingId) {}
