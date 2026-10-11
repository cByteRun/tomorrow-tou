package edu.tou.shared;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class RecordEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Version public long version;
}
