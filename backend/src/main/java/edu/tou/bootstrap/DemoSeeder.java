package edu.tou.bootstrap;

import edu.tou.attendance.Attendance;
import edu.tou.audit.AuditService;
import edu.tou.curriculum.*;
import edu.tou.grades.Grade;
import edu.tou.groups.StudyGroup;
import edu.tou.identity.*;
import edu.tou.schedule.*;
import edu.tou.students.Student;
import edu.tou.teachers.Teacher;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoSeeder implements ApplicationRunner {
  private final EntityManager em;
  private final PasswordEncoder passwords;
  private final AuditService audit;

  @Value("${tou.demo-seed}")
  boolean enabled;

  @Value("${tou.demo-password}")
  String password;

  public DemoSeeder(EntityManager em, PasswordEncoder p, AuditService a) {
    this.em = em;
    passwords = p;
    audit = a;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!enabled || em.createQuery("select count(u) from User u", Long.class).getSingleResult() > 0)
      return;
    if (password.length() < 8 || password.length() > 72)
      throw new IllegalStateException("DEMO_PASSWORD must have 8–72 characters");
    String hash = passwords.encode(password);
    Subject programming = subject("Программирование", "CS-301", 5),
        database = subject("Базы данных", "DB-201", 4),
        math = subject("Высшая математика", "MA-101", 5);
    Teacher
        teacher =
            teacher(
                "Айгуль Садыкова",
                "teacher@tou.edu.kz",
                "Информационные технологии",
                Set.of(programming, database)),
        other = teacher("Руслан Омаров", "omarov@tou.edu.kz", "Математика", Set.of(math));
    StudyGroup group = group("CS-301", 3, teacher, Set.of(programming, database, math)),
        second = group("CS-201", 2, other, Set.of(math));
    Student main = null;
    String[] names = {
      "Данияр Ткачёв",
      "Алина Садыкова",
      "Руслан Ибраев",
      "Мадина Жаксылыкова",
      "Арман Ибраев",
      "Аружан Касымова",
      "Алексей Иванов",
      "Анна Петрова",
      "Дамир Омаров",
      "Алия Нурова"
    };
    List<Student> students = new ArrayList<>();
    for (int i = 0; i < names.length; i++) {
      Student s = new Student();
      String[] n = names[i].split(" ");
      s.firstName = n[0];
      s.lastName = n[1];
      s.studentNo = "TOU-" + (3001 + i);
      s.email = i == 0 ? "student@tou.edu.kz" : "student" + i + "@tou.edu.kz";
      s.phone = "";
      s.group = i < 8 ? group : second;
      em.persist(s);
      students.add(s);
      if (i == 0) main = s;
    }
    Room room = room("A-301", "Главный", 32), r2 = room("B-204", "Учебный", 24);
    Semester sem = new Semester();
    sem.name = "Текущий учебный семестр";
    sem.start = LocalDate.now().minusMonths(3);
    sem.end = LocalDate.now().plusMonths(3);
    sem.status = "active";
    em.persist(sem);
    for (int day = -7; day <= 7; day++) {
      Lesson l = new Lesson();
      l.date = LocalDate.now().plusDays(day);
      l.start = LocalTime.of(9, 0);
      l.end = LocalTime.of(10, 30);
      l.semester = sem;
      l.group = group;
      l.subject = day % 2 == 0 ? programming : database;
      l.teacher = teacher;
      l.room = room;
      l.topic = "Практическое занятие";
      l.status = day < 0 ? "conducted" : "scheduled";
      em.persist(l);
      if (day <= 0)
        for (int i = 0; i < 8; i++) {
          Attendance a = new Attendance();
          a.student = students.get(i);
          a.lesson = l;
          a.status = i == 3 ? "absent" : i == 5 ? "late" : "present";
          a.updatedAt = Instant.now();
          em.persist(a);
          Grade g = new Grade();
          g.student = a.student;
          g.lesson = l;
          g.type = "Текущая";
          g.value = 65 + (i * 3 + day + 7) % 30;
          g.updatedAt = Instant.now();
          em.persist(g);
        }
    }
    Lesson l = new Lesson();
    l.date = LocalDate.now();
    l.start = LocalTime.of(11, 0);
    l.end = LocalTime.of(12, 30);
    l.semester = sem;
    l.group = second;
    l.subject = math;
    l.teacher = other;
    l.room = r2;
    l.topic = "Лекция";
    em.persist(l);
    user("Администратор", "admin@tou.edu.kz", Role.ADMIN, null, null, hash);
    user(teacher.name, teacher.email, Role.TEACHER, null, teacher, hash);
    user(main.firstName + " " + main.lastName, main.email, Role.STUDENT, main, null, hash);
    user("Сотрудник учебного отдела", "staff@tou.edu.kz", Role.STAFF, null, null, hash);
    audit.record("system", "SEED", "database", "Созданы демонстрационные данные");
  }

  private Subject subject(String name, String code, int credits) {
    Subject s = new Subject();
    s.name = name;
    s.code = code;
    s.credits = credits;
    em.persist(s);
    return s;
  }

  private Teacher teacher(String name, String email, String department, Set<Subject> subjects) {
    Teacher t = new Teacher();
    t.name = name;
    t.email = email;
    t.department = department;
    t.subjects.addAll(subjects);
    em.persist(t);
    return t;
  }

  private StudyGroup group(String name, int year, Teacher t, Set<Subject> subjects) {
    StudyGroup g = new StudyGroup();
    g.name = name;
    g.year = year;
    g.curator = t;
    g.subjects.addAll(subjects);
    em.persist(g);
    return g;
  }

  private Room room(String name, String building, int capacity) {
    Room r = new Room();
    r.name = name;
    r.building = building;
    r.capacity = capacity;
    em.persist(r);
    return r;
  }

  private void user(
      String name, String email, Role role, Student student, Teacher teacher, String hash) {
    User u = new User();
    u.name = name;
    u.email = email;
    u.role = role;
    u.student = student;
    u.teacher = teacher;
    u.passwordHash = hash;
    em.persist(u);
  }
}
