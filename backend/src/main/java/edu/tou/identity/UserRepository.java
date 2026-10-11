package edu.tou.identity;

import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  @Query("select u from User u where lower(u.email)=lower(:email)")
  Optional<User> findByEmailIgnoreCase(@Param("email") String email);
}
