package hr.tvz.experimate.experimate.model.reservation;

import hr.tvz.experimate.experimate.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ReservationRepo extends JpaRepository<Reservation, Integer> {
    boolean existsByGuestAndTourListing_MeetingDateBetween(User guest, LocalDateTime start, LocalDateTime end);
}
