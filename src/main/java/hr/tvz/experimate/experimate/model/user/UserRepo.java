package hr.tvz.experimate.experimate.model.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User,Integer> {
    boolean existsByUsername(String username);
    boolean existsByIdNumber(String idNumber);
}
