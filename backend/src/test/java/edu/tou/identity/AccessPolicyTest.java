package edu.tou.identity;

import static edu.tou.shared.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import edu.tou.attendance.Attendance;
import edu.tou.audit.AuditEvent;
import edu.tou.grades.Grade;
import edu.tou.schedule.Lesson;
import edu.tou.shared.*;
import edu.tou.supervision.LessonReview;
import java.util.*;
import org.junit.jupiter.api.*;

class AccessPolicyTest {
  private final Lesson l = lesson(1);

  private Grade grade(long studentId) {
    Grade g = new Grade();
    g.id = 5L;
    g.lesson = l;
    g.student = student(studentId, l);
    return g;
  }

  @Test
  void studentReadsOwnGrade() {
    assertTrue(AccessPolicy.read(user(Role.STUDENT, l), grade(1), List.of(l)));
  }

  @Test
  void studentCannotReadAnotherGrade() {
    assertFalse(AccessPolicy.read(user(Role.STUDENT, l), grade(2), List.of(l)));
  }

  @Test
  void studentCannotWriteOwnGrade() {
    assertFalse(AccessPolicy.write(user(Role.STUDENT, l), "grades", grade(1)));
  }

  @Test
  void teacherReadsOwnLessonGrades() {
    assertTrue(AccessPolicy.read(user(Role.TEACHER, l), grade(1), List.of(l)));
  }

  @Test
  void teacherCannotReadOtherTeachersGrades() {
    User u = user(Role.TEACHER, lesson(2));
    u.teacher.id = 2L;
    assertFalse(AccessPolicy.read(u, grade(1), List.of(l)));
  }

  @Test
  void teacherCannotMoveGradeToAnotherLesson() {
    User u = user(Role.TEACHER, lesson(2));
    u.teacher.id = 2L;
    assertFalse(AccessPolicy.write(u, "grades", grade(1)));
  }

  @Test
  void teacherCanCreateGrades() {
    assertTrue(AccessPolicy.write(user(Role.TEACHER, l), "grades", null));
  }

  @Test
  void teacherCannotCreateUsers() {
    assertFalse(AccessPolicy.write(user(Role.TEACHER, l), "users", null));
  }

  @Test
  void teacherSeesAssignedSubjectWithoutScheduledLessons() {
    assertTrue(AccessPolicy.read(user(Role.TEACHER, l), l.subject, List.of()));
  }

  @Test
  void teacherCannotSeeUnassignedSubjects() {
    var s = new edu.tou.curriculum.Subject();
    s.id = 2L;
    assertFalse(AccessPolicy.read(user(Role.TEACHER, l), s, List.of(l)));
  }

  @Test
  void studentReadsOwnAttendance() {
    Attendance a = new Attendance();
    a.lesson = l;
    a.student = student(1, l);
    assertTrue(AccessPolicy.read(user(Role.STUDENT, l), a, List.of(l)));
  }

  @Test
  void studentCannotReadAnotherAttendance() {
    Attendance a = new Attendance();
    a.lesson = l;
    a.student = student(2, l);
    assertFalse(AccessPolicy.read(user(Role.STUDENT, l), a, List.of(l)));
  }

  @Test
  void studentCannotReadOtherGroupSchedule() {
    User u = user(Role.STUDENT, lesson(2));
    u.student.group.id = 2L;
    assertFalse(AccessPolicy.lesson(u, l));
  }

  @Test
  void staffReadsTeacherSchedule() {
    assertTrue(AccessPolicy.lesson(user(Role.STAFF, l), l));
  }

  @Test
  void staffCannotManageUsers() {
    assertFalse(AccessPolicy.write(user(Role.STAFF, l), "users", null));
  }

  @Test
  void staffCannotChangeGrades() {
    assertFalse(AccessPolicy.write(user(Role.STAFF, l), "grades", grade(1)));
  }

  @Test
  void staffCannotReadStudentGrades() {
    assertFalse(AccessPolicy.read(user(Role.STAFF, l), grade(1), List.of(l)));
  }

  @Test
  void staffCanCreateLessonReviews() {
    assertTrue(AccessPolicy.write(user(Role.STAFF, l), "reviews", null));
  }

  @Test
  void staffCanEditOwnReview() {
    User u = user(Role.STAFF, l);
    LessonReview r = new LessonReview();
    r.reviewer = u;
    r.lesson = l;
    assertTrue(AccessPolicy.write(u, "reviews", r));
  }

  @Test
  void staffCannotEditAnotherEmployeesReview() {
    User u = user(Role.STAFF, l), other = user(Role.STAFF, l);
    other.id = 11L;
    LessonReview r = new LessonReview();
    r.reviewer = other;
    r.lesson = l;
    assertFalse(AccessPolicy.write(u, "reviews", r));
  }

  @Test
  void teacherCannotCreateReview() {
    assertFalse(AccessPolicy.write(user(Role.TEACHER, l), "reviews", null));
  }

  @Test
  void adminCanManageUsers() {
    assertTrue(AccessPolicy.write(user(Role.ADMIN, l), "users", null));
  }

  @Test
  void adminCannotRewriteAudit() {
    assertFalse(AccessPolicy.write(user(Role.ADMIN, l), "audit", new AuditEvent()));
  }

  @Test
  void staffCannotReadAudit() {
    assertFalse(AccessPolicy.read(user(Role.STAFF, l), new AuditEvent(), List.of(l)));
  }
}
