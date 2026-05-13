package hr.tvz.experimate.experimate.domain.reservation.response;

public record PresenceResponse(String username,
                               String firstName,
                               String lastName,
                               String profilePhotoUrl,
                               boolean checkedIn,
                               boolean isHost) {
}
