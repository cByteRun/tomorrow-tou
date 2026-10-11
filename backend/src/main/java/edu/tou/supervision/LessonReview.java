package edu.tou.supervision;

import jakarta.persistence.*;

@Entity
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "lesson_review")
public class LessonReview extends edu.tou.shared.RecordEntity {
  @ManyToOne(optional = false)
  public edu.tou.schedule.Lesson lesson;

  @ManyToOne(optional = false)
  public edu.tou.identity.User reviewer;

  @Column(nullable = false)
  public String result;

  @Column(nullable = false, length = 2000)
  public String note;

  public java.time.Instant updatedAt;
}
