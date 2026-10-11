package edu.tou.university;

import static edu.tou.shared.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.tou.attendance.Attendance;
import edu.tou.audit.AuditService;
import edu.tou.grades.Grade;
import edu.tou.identity.*;
import edu.tou.schedule.*;
import edu.tou.shared.*;
import edu.tou.students.Student;
import edu.tou.supervision.LessonReview;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings({"unchecked", "rawtypes"})
class UniversityServiceTest {
  private EntityManager em;
  private IdentityService identity;
  private UniversityService service;
  private User actor;
  private final Map<Class<?>, List<RecordEntity>> records = new HashMap<>();
  private Lesson own;

  @BeforeEach
  void setup() {
    em = mock(EntityManager.class);
    identity = mock(IdentityService.class);
    service =
        new UniversityService(
            identity,
            mock(PasswordEncoder.class),
            mock(AuditService.class),
            mock(SessionRepository.class));
    ReflectionTestUtils.setField(service, "em", em);
    own = lesson(1);
    actor = user(Role.ADMIN, own);
    when(identity.current()).thenAnswer(i -> actor);
    put(own);
    put(own.subject);
    put(own.teacher);
    put(own.group);
    put(own.room);
    put(own.semester);
    put(student(1, own));
    when(em.find(any(), any()))
        .thenAnswer(
            i ->
                records.getOrDefault(i.getArgument(0), List.of()).stream()
                    .filter(e -> Objects.equals(e.id, i.getArgument(1)))
                    .findFirst()
                    .orElse(null));
    when(em.createQuery(anyString(), any(Class.class)))
        .thenAnswer(
            i -> {
              TypedQuery q = mock(TypedQuery.class);
              when(q.getResultList())
                  .thenAnswer(j -> records.getOrDefault(i.getArgument(1), List.of()));
              return q;
            });
    doAnswer(
            i -> {
              RecordEntity row = i.getArgument(0);
              row.id = 99L;
              put(row);
              return null;
            })
        .when(em)
        .persist(any());
  }

  private void put(RecordEntity e) {
    records.computeIfAbsent(e.getClass(), k -> new ArrayList<>()).add(e);
  }

  private Map<String, Object> gradeInput(long studentId, long lessonId, int value) {
    return new LinkedHashMap<>(
        Map.of("studentId", studentId, "lessonId", lessonId, "value", value, "type", "Текущая"));
  }

  @Test
  void studentCannotCreateGradeThroughService() {
    actor = user(Role.STUDENT, own);
    BusinessException e =
        assertThrows(
            BusinessException.class, () -> service.save("grades", null, gradeInput(1, 1, 90)));
    assertEquals(403, e.status);
    verify(em, never()).persist(any());
  }

  @Test
  void teacherCannotCreateUserThroughService() {
    actor = user(Role.TEACHER, own);
    assertThrows(BusinessException.class, () -> service.save("users", null, Map.of()));
    verify(em, never()).persist(any());
  }

  @Test
  void staffCanPersistReviewWithOwnIdentity() {
    actor = user(Role.STAFF, own);
    own.date = LocalDate.now().minusDays(1);
    Map<String, Object> result =
        service.save(
            "reviews",
            null,
            Map.of("lessonId", 1, "result", "confirmed", "note", "Занятие проведено"));
    assertEquals(99L, result.get("id"));
    LessonReview r = (LessonReview) records.get(LessonReview.class).getFirst();
    assertSame(actor, r.reviewer);
    verify(em).persist(r);
  }

  @Test
  void futureLessonReviewIsRejected() {
    actor = user(Role.STAFF, own);
    own.date = LocalDate.now().plusDays(1);
    assertThrows(
        BusinessException.class,
        () ->
            service.save(
                "reviews", null, Map.of("lessonId", 1, "result", "confirmed", "note", "Проверка")));
    verify(em, never()).persist(any());
  }

  @Test
  void studentOutsideLessonGroupIsRejected() {
    Student other = student(2, lesson(2));
    other.group.id = 2L;
    put(other);
    assertThrows(BusinessException.class, () -> service.save("grades", null, gradeInput(2, 1, 90)));
    verify(em, never()).persist(any());
  }

  @Test
  void futureAttendanceIsRejected() {
    own.date = LocalDate.now().plusDays(1);
    assertThrows(
        BusinessException.class,
        () ->
            service.save(
                "attendance", null, Map.of("studentId", 1, "lessonId", 1, "status", "present")));
  }

  @Test
  void cancelledLessonCannotReceiveGrades() {
    own.status = "cancelled";
    assertThrows(BusinessException.class, () -> service.save("grades", null, gradeInput(1, 1, 90)));
  }

  @Test
  void requiredGradeValueCannotBeOmitted() {
    var body = gradeInput(1, 1, 0);
    body.remove("value");
    assertThrows(BusinessException.class, () -> service.save("grades", null, body));
  }

  @Test
  void fractionalGradeCannotBeTruncated() {
    var body = gradeInput(1, 1, 0);
    body.put("value", 80.5);
    assertThrows(NumberFormatException.class, () -> service.save("grades", null, body));
  }

  @Test
  void updateRequiresVersion() {
    Grade g = new Grade();
    g.id = 3L;
    g.lesson = own;
    g.student = student(1, own);
    put(g);
    assertThrows(BusinessException.class, () -> service.save("grades", 3L, gradeInput(1, 1, 90)));
    verify(em, never()).flush();
  }

  @Test
  void staleVersionIsRejected() {
    Grade g = new Grade();
    g.id = 3L;
    g.version = 2;
    g.lesson = own;
    g.student = student(1, own);
    put(g);
    var body = gradeInput(1, 1, 90);
    body.put("version", 1);
    assertEquals(
        409, assertThrows(BusinessException.class, () -> service.save("grades", 3L, body)).status);
    verify(em, never()).flush();
  }

  @Test
  void changingLessonCannotBypassTeacherScope() {
    actor = user(Role.TEACHER, own);
    Grade g = new Grade();
    g.id = 3L;
    g.lesson = own;
    g.student = student(1, own);
    put(g);
    Lesson other = lesson(2);
    other.teacher.id = 2L;
    put(other);
    var body = gradeInput(1, 2, 90);
    body.put("version", 0);
    assertEquals(
        403, assertThrows(BusinessException.class, () -> service.save("grades", 3L, body)).status);
    verify(em, never()).flush();
  }

  @Test
  void adminCannotDeleteOwnAccount() {
    put(actor);
    assertThrows(BusinessException.class, () -> service.delete("users", actor.id, 0));
    verify(em, never()).remove(any());
  }

  @Test
  void deletingStaleRecordIsRejected() {
    put(actor);
    Room r = new Room();
    r.id = 5L;
    r.version = 1;
    put(r);
    assertThrows(BusinessException.class, () -> service.delete("rooms", 5, 0));
    verify(em, never()).remove(any());
  }

  @Test
  void teacherStudentAverageExcludesOtherTeachersGrades() {
    actor = user(Role.TEACHER, own);
    Student s = student(1, own);
    Grade a = new Grade();
    a.id = 1L;
    a.student = s;
    a.lesson = own;
    a.value = 100;
    put(a);
    Grade b = new Grade();
    b.id = 2L;
    b.student = s;
    b.lesson = lesson(2);
    b.lesson.teacher.id = 2L;
    b.value = 0;
    put(b);
    assertEquals(100.0, service.dto(s).get("avg"));
  }

  @Test
  void studentCannotGetAnotherStudentsRecord() {
    actor = user(Role.STUDENT, own);
    put(student(2, own));
    assertEquals(
        403, assertThrows(BusinessException.class, () -> service.get("students", 2)).status);
  }

  @Test
  void staffCannotMarkWholeRoster() {
    actor = user(Role.STAFF, own);
    assertThrows(BusinessException.class, () -> service.markAll(1));
    verify(em, never()).persist(any());
  }

  @Test
  void bulkMarkPreservesExistingAbsence() {
    Attendance absent = new Attendance();
    absent.id = 3L;
    absent.lesson = own;
    absent.student = student(1, own);
    absent.status = "absent";
    put(absent);
    own.date = LocalDate.now().minusDays(1);
    service.markAll(1);
    assertEquals("absent", absent.status);
    verify(em, never()).persist(any());
  }
}
