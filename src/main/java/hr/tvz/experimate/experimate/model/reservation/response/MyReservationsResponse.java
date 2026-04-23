package hr.tvz.experimate.experimate.model.reservation.response;

import java.util.List;

public record MyReservationsResponse(
        List<ReservationResponse> asGuest,
        List<ReservationResponse> asHost
) {}
