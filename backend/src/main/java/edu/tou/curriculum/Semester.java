package edu.tou.curriculum;

import jakarta.persistence.*;

@Entity
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "semester")
public class Semester extends edu.tou.shared.RecordEntity {
  @Column(nullable = false)
  public String name;

  @Column(name = "start_date", nullable = false)
  public java.time.LocalDate start;

  @Column(name = "end_date", nullable = false)
  public java.time.LocalDate end;

  @Column(nullable = false)
  public String status;
}
