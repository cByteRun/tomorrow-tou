package edu.tou.grades;

import jakarta.persistence.*;

@Entity
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "grade")
public class Grade extends edu.tou.shared.RecordEntity {
  @ManyToOne(optional = false)
  public edu.tou.students.Student student;

  @ManyToOne(optional = false)
  public edu.tou.schedule.Lesson lesson;

  @Column(nullable = false)
  public String type;

  @Column(name = "score", nullable = false)
  public int value;

  public java.time.Instant updatedAt;
}
