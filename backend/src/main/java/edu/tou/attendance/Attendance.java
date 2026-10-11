package edu.tou.attendance;

import jakarta.persistence.*;

@Entity
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "attendance")
public class Attendance extends edu.tou.shared.RecordEntity {
  @ManyToOne(optional = false)
  public edu.tou.students.Student student;

  @ManyToOne(optional = false)
  public edu.tou.schedule.Lesson lesson;

  @Column(nullable = false)
  public String status;

  public java.time.Instant updatedAt;
}
