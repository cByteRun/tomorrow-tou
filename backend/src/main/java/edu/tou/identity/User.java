package edu.tou.identity;

import jakarta.persistence.*;

@Entity
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "app_user")
public class User extends edu.tou.shared.RecordEntity {
  @Column(nullable = false)
  public String name;

  @Column(nullable = false, unique = true)
  public String email;

  @Column(nullable = false)
  public String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Role role;

  @OneToOne public edu.tou.students.Student student;
  @OneToOne public edu.tou.teachers.Teacher teacher;
  public boolean enabled = true;
}
