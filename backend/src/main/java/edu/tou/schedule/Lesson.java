package edu.tou.schedule;

import jakarta.persistence.*;

@Entity
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "lesson")
public class Lesson extends edu.tou.shared.RecordEntity {
  @Column(name = "lesson_date", nullable = false)
  public java.time.LocalDate date;

  @Column(name = "start_time", nullable = false)
  public java.time.LocalTime start;

  @Column(name = "end_time", nullable = false)
  public java.time.LocalTime end;

  @ManyToOne(optional = false)
  public edu.tou.curriculum.Semester semester;

  @ManyToOne(optional = false)
  public edu.tou.groups.StudyGroup group;

  @ManyToOne(optional = false)
  public edu.tou.curriculum.Subject subject;

  @ManyToOne(optional = false)
  public edu.tou.teachers.Teacher teacher;

  @ManyToOne(optional = false)
  public Room room;

  public String topic;

  @Column(nullable = false)
  public String status = "scheduled";
}
