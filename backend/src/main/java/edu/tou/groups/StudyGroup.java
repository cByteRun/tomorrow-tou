package edu.tou.groups;

import jakarta.persistence.*;

@Entity
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "study_group")
public class StudyGroup extends edu.tou.shared.RecordEntity {
  @Column(nullable = false, unique = true)
  public String name;

  @Column(name = "study_year", nullable = false)
  public int year;

  @ManyToOne(optional = false)
  public edu.tou.teachers.Teacher curator;

  @ManyToMany
  @JoinTable(
      name = "group_subject",
      joinColumns = @JoinColumn(name = "group_id"),
      inverseJoinColumns = @JoinColumn(name = "subject_id"))
  public java.util.Set<edu.tou.curriculum.Subject> subjects = new java.util.LinkedHashSet<>();
}
