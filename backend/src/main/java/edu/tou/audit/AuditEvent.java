package edu.tou.audit;

import jakarta.persistence.*;

@Entity
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "audit_event")
public class AuditEvent extends edu.tou.shared.RecordEntity {
  @Column(nullable = false)
  public java.time.Instant at;

  @Column(nullable = false)
  public String actor;

  @Column(nullable = false)
  public String action;

  @Column(nullable = false)
  public String entity;

  @Column(nullable = false, length = 2000)
  public String description;
}
