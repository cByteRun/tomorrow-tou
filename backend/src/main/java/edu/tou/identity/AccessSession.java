package edu.tou.identity;

import jakarta.persistence.*;

@Entity
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "access_session")
public class AccessSession extends edu.tou.shared.RecordEntity {
  @Column(nullable = false, unique = true)
  public String tokenHash;

  @ManyToOne(optional = false)
  public User user;

  @Column(nullable = false)
  public java.time.Instant expiresAt;
}
