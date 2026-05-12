package hr.tvz.experimate.experimate.model.reservation.response;

public record PresenceResponse(String username,
                               String firstName,
                               String lastName,
                               String profilePhotoUrl,
                               boolean checkedIn,
                               boolean isHost) {
}
