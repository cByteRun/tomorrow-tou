package edu.tou.teachers;

import jakarta.persistence.*;

@Entity
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "teacher")
public class Teacher extends edu.tou.shared.RecordEntity {
  @Column(nullable = false)
  public String name;

  @Column(nullable = false, unique = true)
  public String email;

  @Column(nullable = false)
  public String department;

  @ManyToMany
  @JoinTable(
      name = "teacher_subject",
      joinColumns = @JoinColumn(name = "teacher_id"),
      inverseJoinColumns = @JoinColumn(name = "subject_id"))
  public java.util.Set<edu.tou.curriculum.Subject> subjects = new java.util.LinkedHashSet<>();
}
