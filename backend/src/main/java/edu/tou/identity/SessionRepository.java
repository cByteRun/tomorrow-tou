package edu.tou.identity;

import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface SessionRepository extends JpaRepository<AccessSession, Long> {
  @EntityGraph(attributePaths = {"user"})
  Optional<AccessSession> findByTokenHash(String hash);

  void deleteByUserId(Long id);
}
