package edu.tou.curriculum;

import jakarta.persistence.*;

@Entity
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "subject")
public class Subject extends edu.tou.shared.RecordEntity {
  @Column(nullable = false)
  public String name;

  @Column(nullable = false, unique = true)
  public String code;

  public int credits;
}
