package hr.tvz.experimate.experimate.model.tour_listing;

import hr.tvz.experimate.experimate.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface TourListingRepo extends JpaRepository<TourListing, Integer> {
    boolean existsByHostAndMeetingDateBetween(User user, LocalDateTime start, LocalDateTime end);
    boolean existsByHost_Id(Integer id);
    int deleteAllByHost_Id(Integer id);

}
