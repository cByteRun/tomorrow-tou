package edu.tou.university;

import static edu.tou.shared.BusinessException.require;

import edu.tou.attendance.Attendance;
import edu.tou.audit.AuditService;
import edu.tou.curriculum.*;
import edu.tou.grades.*;
import edu.tou.groups.StudyGroup;
import edu.tou.identity.*;
import edu.tou.schedule.*;
import edu.tou.shared.*;
import edu.tou.students.Student;
import edu.tou.supervision.LessonReview;
import edu.tou.teachers.Teacher;
import jakarta.persistence.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UniversityService {
  @PersistenceContext private EntityManager em;
  private final IdentityService identity;
  private final PasswordEncoder passwords;
  private final AuditService audit;
  private final SessionRepository sessions;

  public UniversityService(
      IdentityService i, PasswordEncoder p, AuditService a, SessionRepository s) {
    identity = i;
    passwords = p;
    audit = a;
    sessions = s;
  }

  public <T extends RecordEntity> List<T> all(Class<T> t) {
    return em.createQuery("select e from " + t.getSimpleName() + " e order by e.id", t)
        .getResultList();
  }

  public List<RecordEntity> visible(String kind) {
    User u = identity.current();
    List<Lesson> lessons = all(Lesson.class);
    return new ArrayList<>(
        all(EntityCatalog.type(kind)).stream()
            .filter(e -> AccessPolicy.read(u, e, lessons))
            .toList());
  }

  public List<Map<String, Object>> list(String kind) {
    return visible(kind).stream().map(this::dto).toList();
  }

  public Map<String, Object> get(String kind, long id) {
    RecordEntity row = find(EntityCatalog.type(kind), id);
    if (!AccessPolicy.read(identity.current(), row, all(Lesson.class))) AccessPolicy.deny();
    return dto(row);
  }

  public <T extends RecordEntity> T find(Class<T> t, Object id) {
    if (id == null || id.toString().isBlank())
      throw new BusinessException("Выберите связанную запись");
    T e = em.find(t, Long.valueOf(id.toString()));
    if (e == null) throw new BusinessException(404, "Связанная запись не найдена");
    return e;
  }

  public Map<String, Object> dto(RecordEntity e) {
    if (e instanceof User u) return identity.view(u);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("id", e.id);
    out.put("version", e.version);
    try {
      for (Field f : e.getClass().getFields()) {
        if (f.getDeclaringClass() == RecordEntity.class) continue;
        Object v = f.get(e);
        if (v instanceof RecordEntity r) out.put(f.getName() + "Id", r.id);
        else if (v instanceof Collection<?> c)
          out.put(
              f.getName().equals("subjects") ? "subjectIds" : f.getName(),
              c.stream().map(x -> ((RecordEntity) x).id).toList());
        else
          out.put(f.getName(), v instanceof java.time.temporal.TemporalAccessor ? v.toString() : v);
      }
    } catch (IllegalAccessException ex) {
      throw new IllegalStateException(ex);
    }
    if (e instanceof Student s) {
      out.put("fullName", s.firstName + " " + s.lastName);
      out.put("groupName", s.group.name);
      out.put(
          "avg",
          GradeRules.average(
              all(Grade.class).stream()
                  .filter(
                      g ->
                          g.student.id.equals(s.id)
                              && AccessPolicy.read(identity.current(), g, List.of()))
                  .toList()));
    }
    if (e instanceof Teacher t) {
      out.put("subjectNames", t.subjects.stream().map(s -> s.name).toList());
      out.put(
          "load",
          all(Lesson.class).stream()
              .filter(l -> l.teacher.id.equals(t.id) && !l.status.equals("cancelled"))
              .count());
      out.put(
          "hours",
          all(Lesson.class).stream()
                  .filter(l -> l.teacher.id.equals(t.id) && !l.status.equals("cancelled"))
                  .mapToLong(l -> Duration.between(l.start, l.end).toMinutes())
                  .sum()
              / 60.0);
    }
    if (e instanceof StudyGroup g) {
      out.put("curatorName", g.curator.name);
      out.put("subjectNames", g.subjects.stream().map(s -> s.name).toList());
      out.put(
          "studentCount", all(Student.class).stream().filter(s -> s.group.id.equals(g.id)).count());
      List<Grade> scores =
          all(Grade.class).stream()
              .filter(
                  x ->
                      x.student.group.id.equals(g.id)
                          && AccessPolicy.read(identity.current(), x, List.of()))
              .toList();
      out.put("avg", GradeRules.average(scores));
    }
    if (e instanceof Subject s) {
      out.put(
          "teacherCount", all(Teacher.class).stream().filter(t -> t.subjects.contains(s)).count());
      out.put(
          "avg",
          GradeRules.average(
              all(Grade.class).stream()
                  .filter(
                      g ->
                          g.lesson.subject.id.equals(s.id)
                              && AccessPolicy.read(identity.current(), g, List.of()))
                  .toList()));
    }
    if (e instanceof Lesson l) {
      out.put("groupName", l.group.name);
      out.put("teacherName", l.teacher.name);
      out.put("subjectName", l.subject.name);
      out.put("roomName", l.room.name);
    }
    if (e instanceof Grade g) {
      out.put("studentName", g.student.firstName + " " + g.student.lastName);
      out.put("subjectName", g.lesson.subject.name);
      out.put("date", g.lesson.date.toString());
      out.put("groupName", g.student.group.name);
      out.put("semesterId", g.lesson.semester.id);
      out.put("groupId", g.student.group.id);
    }
    if (e instanceof Attendance a) {
      out.put("studentName", a.student.firstName + " " + a.student.lastName);
      out.put("subjectName", a.lesson.subject.name);
      out.put("date", a.lesson.date.toString());
    }
    if (e instanceof LessonReview r) {
      out.put("teacherName", r.lesson.teacher.name);
      out.put("subjectName", r.lesson.subject.name);
      out.put("date", r.lesson.date.toString());
      out.put("reviewerName", r.reviewer.name);
    }
    return out;
  }

  public Map<String, Object> save(String kind, Long id, Map<String, Object> input) {
    Class<? extends RecordEntity> type = EntityCatalog.type(kind);
    User actor = identity.current();
    RecordEntity row;
    try {
      row = id == null ? type.getConstructor().newInstance() : find(type, id);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
    if (!AccessPolicy.write(actor, kind, row.id == null ? null : row)) AccessPolicy.deny();
    if (id != null) {
      require(input.containsKey("version"), "Обновите карточку перед сохранением");
      if (row.version != Long.parseLong(input.get("version").toString()))
        throw new BusinessException(
            409, "Запись изменена другим пользователем. Обновите страницу.");
    }
    if (kind.equals("grades")) require(input.containsKey("value"), "Укажите балл");
    if (row instanceof LessonReview r && id == null) r.reviewer = actor;
    if (row instanceof User u) bindUser(u, input);
    else bind(row, input);
    // Authorization is repeated after binding, so changing lessonId cannot transfer a teacher's
    // record to another teacher.
    if (!AccessPolicy.write(actor, kind, row)) AccessPolicy.deny();
    validate(row, actor);
    if (row instanceof User u && id != null) sessions.deleteByUserId(u.id);
    if (id == null) em.persist(row);
    em.flush();
    audit.record(actor.email, id == null ? "CREATE" : "UPDATE", kind, "Запись #" + row.id);
    return dto(row);
  }

  private void bindUser(User u, Map<String, Object> b) {
    u.name = text(b, "name");
    u.email = email(b);
    u.role = Role.parse(text(b, "role"));
    u.enabled = !Boolean.FALSE.equals(b.getOrDefault("enabled", true));
    u.student = u.role == Role.STUDENT ? find(Student.class, b.get("entityId")) : null;
    u.teacher = u.role == Role.TEACHER ? find(Teacher.class, b.get("entityId")) : null;
    String p = Objects.toString(b.get("password"), "");
    if (u.id == null || !p.isBlank()) {
      require(p.length() >= 8 && p.length() <= 72, "Пароль должен содержать 8–72 символа");
      u.passwordHash = passwords.encode(p);
    }
  }

  @SuppressWarnings("unchecked")
  private void bind(RecordEntity e, Map<String, Object> b) {
    try {
      for (Field f : e.getClass().getDeclaredFields()) {
        if (f.getName().equals("updatedAt") || f.getName().equals("reviewer")) continue;
        String key =
            RecordEntity.class.isAssignableFrom(f.getType())
                ? f.getName() + "Id"
                : f.getName().equals("subjects") ? "subjectIds" : f.getName();
        if (!b.containsKey(key)) continue;
        Object v = b.get(key);
        if (RecordEntity.class.isAssignableFrom(f.getType()))
          f.set(e, find((Class<? extends RecordEntity>) f.getType(), v));
        else if (f.getType() == Set.class) {
          require(v instanceof List<?>, "Выберите дисциплины");
          Set<Subject> set = new LinkedHashSet<>();
          for (Object id : (List<?>) v) set.add(find(Subject.class, id));
          f.set(e, set);
        } else if (f.getType() == String.class) f.set(e, v == null ? null : v.toString().trim());
        else if (f.getType() == int.class) f.set(e, Integer.parseInt(Objects.toString(v, "")));
        else if (f.getType() == LocalDate.class) f.set(e, LocalDate.parse(v.toString()));
        else if (f.getType() == LocalTime.class) f.set(e, LocalTime.parse(v.toString()));
      }
    } catch (IllegalAccessException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private String fieldLabel(String key) {
    return switch (key) {
      case "name" -> "название / имя";
      case "email" -> "электронная почта";
      case "firstName" -> "имя";
      case "lastName" -> "фамилия";
      case "studentNo" -> "номер студента";
      case "department" -> "кафедра";
      case "group" -> "группа";
      case "curator" -> "куратор";
      case "teacher" -> "преподаватель";
      case "subject" -> "дисциплина";
      case "room" -> "аудитория";
      case "semester" -> "семестр";
      case "student" -> "студент";
      case "lesson" -> "занятие";
      case "date" -> "дата";
      case "start" -> "начало";
      case "end" -> "окончание";
      case "status" -> "статус";
      case "result" -> "результат";
      case "note" -> "комментарий";
      case "code" -> "код";
      case "building" -> "корпус";
      case "type" -> "вид оценки";
      default -> key;
    };
  }

  private String text(Map<String, Object> b, String key) {
    String s = Objects.toString(b.get(key), "").trim();
    require(!s.isEmpty() && s.length() <= 255, "Заполните поле " + key + " (до 255 символов)");
    return s;
  }

  private String email(Map<String, Object> b) {
    String s = text(b, "email").toLowerCase(Locale.ROOT);
    require(s.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"), "Некорректная электронная почта");
    return s;
  }

  private void validate(RecordEntity e, User actor) {
    try {
      for (Field f : e.getClass().getDeclaredFields()) {
        Column c = f.getAnnotation(Column.class);
        if (c != null && !c.nullable() && !f.getName().equals("updatedAt")) {
          Object v = f.get(e);
          require(
              v != null && (!(v instanceof String s) || !s.isBlank() && s.length() <= c.length()),
              "Заполните обязательное поле: " + fieldLabel(f.getName()));
        }
        ManyToOne m = f.getAnnotation(ManyToOne.class);
        if (m != null && !m.optional() && !f.getName().equals("reviewer"))
          require(f.get(e) != null, "Выберите: " + fieldLabel(f.getName()));
      }
    } catch (IllegalAccessException ex) {
      throw new IllegalStateException(ex);
    }
    if (e instanceof User u) {
      require(
          !Objects.equals(u.id, actor.id) || u.role == Role.ADMIN && u.enabled,
          "Нельзя изменить свою роль или отключить свой аккаунт");
    }
    if (e instanceof Student s) {
      s.email = email(Map.of("email", s.email));
      if (s.id != null) {
        require(
            all(Grade.class).stream()
                .noneMatch(g -> g.student.id.equals(s.id) && !g.lesson.group.id.equals(s.group.id)),
            "Студент связан с оценками другой группы");
        require(
            all(Attendance.class).stream()
                .noneMatch(a -> a.student.id.equals(s.id) && !a.lesson.group.id.equals(s.group.id)),
            "Студент связан с посещаемостью другой группы");
      }
      long size =
          all(Student.class).stream()
                  .filter(x -> x.group.id.equals(s.group.id) && !Objects.equals(x.id, s.id))
                  .count()
              + 1;
      require(
          all(Lesson.class).stream()
              .filter(l -> l.group.id.equals(s.group.id) && !l.status.equals("cancelled"))
              .allMatch(l -> l.room.capacity >= size),
          "Группа превышает вместимость аудитории в расписании");
    }
    if (e instanceof Teacher t) {
      t.email = email(Map.of("email", t.email));
      require(!t.subjects.isEmpty(), "Выберите дисциплины преподавателя");
      require(
          all(Lesson.class).stream()
              .filter(l -> Objects.equals(l.teacher.id, t.id))
              .allMatch(l -> t.subjects.contains(l.subject)),
          "Дисциплина используется в расписании преподавателя");
    }
    if (e instanceof StudyGroup g) {
      require(g.year >= 1 && g.year <= 6, "Курс должен быть от 1 до 6");
      require(!g.subjects.isEmpty(), "Выберите дисциплины группы");
      require(
          all(Lesson.class).stream()
              .filter(l -> Objects.equals(l.group.id, g.id))
              .allMatch(l -> g.subjects.contains(l.subject)),
          "Дисциплина используется в расписании группы");
    }
    if (e instanceof Subject s)
      require(s.credits >= 1 && s.credits <= 30, "Кредиты должны быть от 1 до 30");
    if (e instanceof Room r) {
      require(r.capacity >= 1 && r.capacity <= 1000, "Вместимость должна быть от 1 до 1000");
      require(
          all(Lesson.class).stream()
              .filter(l -> Objects.equals(l.room.id, r.id) && !l.status.equals("cancelled"))
              .allMatch(
                  l ->
                      all(Student.class).stream().filter(s -> s.group.id.equals(l.group.id)).count()
                          <= r.capacity),
          "Новая вместимость меньше состава назначенной группы");
    }
    if (e instanceof Semester s) {
      require(s.start.isBefore(s.end), "Конец семестра должен быть позже начала");
      require(Set.of("active", "archived").contains(s.status), "Некорректный статус семестра");
      require(
          all(Lesson.class).stream()
              .filter(l -> Objects.equals(l.semester.id, s.id))
              .allMatch(l -> !l.date.isBefore(s.start) && !l.date.isAfter(s.end)),
          "В семестре есть занятия вне новых границ");
    }
    if (e instanceof Lesson l) {
      ScheduleRules.validate(
          l,
          all(Student.class).stream().filter(s -> s.group.id.equals(l.group.id)).count(),
          all(Lesson.class));
    }
    if (e instanceof Grade g) {
      roster(g.student, g.lesson);
      GradeRules.validate(g.value, g.type);
      g.updatedAt = Instant.now();
    }
    if (e instanceof Attendance a) {
      roster(a.student, a.lesson);
      require(
          !a.lesson.date.isAfter(LocalDate.now()), "Нельзя отмечать посещаемость будущего занятия");
      require(
          Set.of("present", "late", "absent", "excused").contains(a.status),
          "Некорректный статус посещаемости");
      a.updatedAt = Instant.now();
    }
    if (e instanceof LessonReview r) {
      require(
          actor.role == Role.STAFF || actor.role == Role.ADMIN,
          "Контроль доступен сотруднику и администратору");
      require(!r.lesson.date.isAfter(LocalDate.now()), "Проверить можно только прошедшее занятие");
      require(
          Set.of("confirmed", "issue", "pending").contains(r.result),
          "Некорректный результат проверки");
      r.reviewer = actor;
      r.updatedAt = Instant.now();
    }
  }

  private void roster(Student s, Lesson l) {
    require(s.group.id.equals(l.group.id), "Студент не состоит в группе занятия");
    require(!l.status.equals("cancelled"), "Занятие отменено");
  }

  public void delete(String kind, long id, long version) {
    User actor = identity.current();
    RecordEntity e = find(EntityCatalog.type(kind), id);
    if (!AccessPolicy.write(actor, kind, e)) AccessPolicy.deny();
    require(e.version == version, "Запись изменена, обновите данные");
    require(!(e instanceof User u && u.id.equals(actor.id)), "Нельзя удалить свой аккаунт");
    em.remove(e);
    em.flush();
    audit.record(actor.email, "DELETE", kind, "Запись #" + id);
  }

  public void markAll(long lessonId) {
    User u = identity.current();
    Lesson l = find(Lesson.class, lessonId);
    if (u.role != Role.ADMIN && !(u.role == Role.TEACHER && u.teacher.id.equals(l.teacher.id)))
      AccessPolicy.deny();
    require(
        !l.date.isAfter(LocalDate.now()) && !l.status.equals("cancelled"),
        "Занятие ещё не состоялось или отменено");
    for (Student s : all(Student.class)) {
      if (!s.group.id.equals(l.group.id)) continue;
      if (all(Attendance.class).stream()
          .anyMatch(a -> a.lesson.id.equals(l.id) && a.student.id.equals(s.id))) continue;
      Attendance a = new Attendance();
      a.lesson = l;
      a.student = s;
      a.status = "present";
      a.updatedAt = Instant.now();
      em.persist(a);
    }
    em.flush();
    audit.record(u.email, "BULK_CREATE", "attendance", "Занятие #" + lessonId);
  }
}
