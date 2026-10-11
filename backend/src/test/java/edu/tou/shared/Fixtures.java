package edu.tou.shared;

import edu.tou.curriculum.*;
import edu.tou.groups.StudyGroup;
import edu.tou.identity.*;
import edu.tou.schedule.*;
import edu.tou.students.Student;
import edu.tou.teachers.Teacher;
import java.time.*;

public final class Fixtures {
  private Fixtures() {}

  public static Lesson lesson(long id) {
    Subject s = new Subject();
    s.id = 1L;
    s.name = "Программирование";
    s.code = "CS";
    s.credits = 5;
    Teacher t = new Teacher();
    t.id = 1L;
    t.name = "Teacher";
    t.subjects.add(s);
    StudyGroup g = new StudyGroup();
    g.id = 1L;
    g.name = "CS-301";
    g.curator = t;
    g.subjects.add(s);
    Room r = new Room();
    r.id = 1L;
    r.name = "A-301";
    r.capacity = 20;
    Semester sem = new Semester();
    sem.id = 1L;
    sem.start = LocalDate.of(2026, 9, 1);
    sem.end = LocalDate.of(2026, 12, 31);
    sem.status = "active";
    Lesson l = new Lesson();
    l.id = id;
    l.date = LocalDate.of(2026, 10, 1);
    l.start = LocalTime.of(9, 0);
    l.end = LocalTime.of(10, 30);
    l.teacher = t;
    l.group = g;
    l.room = r;
    l.subject = s;
    l.semester = sem;
    return l;
  }

  public static Student student(long id, Lesson l) {
    Student s = new Student();
    s.id = id;
    s.firstName = "First";
    s.lastName = "Last";
    s.group = l.group;
    return s;
  }

  public static User user(Role role, Lesson l) {
    User u = new User();
    u.id = 10L;
    u.name = "Test";
    u.email = "test@tou.edu.kz";
    u.role = role;
    u.teacher = role == Role.TEACHER ? l.teacher : null;
    u.student = role == Role.STUDENT ? student(1, l) : null;
    return u;
  }
}
