package edu.tou.schedule;

import jakarta.persistence.*;

@Entity
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "room")
public class Room extends edu.tou.shared.RecordEntity {
  @Column(nullable = false, unique = true)
  public String name;

  @Column(nullable = false)
  public String building;

  public int capacity;
}
