package edu.tou.students;

import jakarta.persistence.*;

@Entity
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "student")
public class Student extends edu.tou.shared.RecordEntity {
  @Column(nullable = false)
  public String firstName;

  @Column(nullable = false)
  public String lastName;

  @Column(nullable = false, unique = true)
  public String studentNo;

  @Column(nullable = false, unique = true)
  public String email;

  public String phone;

  @ManyToOne(optional = false)
  public edu.tou.groups.StudyGroup group;
}
