package hr.tvz.experimate.experimate.model.user;

import hr.tvz.experimate.experimate.model.user.response.UserResponse;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Integer> {
    boolean existsByUsername(String username);

    boolean existsByIdNumber(String idNumber);

    Optional<User> findByUsername(String username);

    List<User> findByUsernameContainingOrFirstNameContainingOrLastNameContaining(String username,
                                                                                 String firstName,
                                                                                 String lastName,
                                                                                 Sort sort);
}
