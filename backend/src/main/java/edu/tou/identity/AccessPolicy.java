package edu.tou.identity;

import edu.tou.attendance.Attendance;
import edu.tou.curriculum.*;
import edu.tou.grades.Grade;
import edu.tou.groups.StudyGroup;
import edu.tou.schedule.*;
import edu.tou.shared.*;
import edu.tou.students.Student;
import edu.tou.supervision.LessonReview;
import edu.tou.teachers.Teacher;
import java.util.List;

public final class AccessPolicy {
  private AccessPolicy() {}

  private static boolean same(RecordEntity a, RecordEntity b) {
    return a != null && b != null && a.id.equals(b.id);
  }

  public static boolean lesson(User u, Lesson l) {
    return u.role == Role.ADMIN
        || u.role == Role.STAFF
        || u.role == Role.TEACHER && same(u.teacher, l.teacher)
        || u.role == Role.STUDENT && u.student != null && same(u.student.group, l.group);
  }

  public static boolean read(User u, RecordEntity row, List<Lesson> lessons) {
    if (u.role == Role.ADMIN) return true;
    if (row instanceof User) return same(u, row);
    if (row instanceof Grade g)
      return u.role == Role.STUDENT
          ? same(u.student, g.student)
          : u.role == Role.TEACHER && same(u.teacher, g.lesson.teacher);
    if (row instanceof Attendance a)
      return u.role == Role.STUDENT
          ? same(u.student, a.student)
          : u.role == Role.TEACHER && same(u.teacher, a.lesson.teacher);
    if (row instanceof Lesson l) return lesson(u, l);
    if (row instanceof LessonReview r)
      return u.role == Role.STAFF || u.role == Role.TEACHER && same(u.teacher, r.lesson.teacher);
    if (row instanceof Teacher t)
      return u.role == Role.STAFF
          || u.role == Role.TEACHER && same(u.teacher, t)
          || u.role == Role.STUDENT
              && lessons.stream().anyMatch(l -> lesson(u, l) && same(t, l.teacher));
    if (row instanceof Student s)
      return u.role == Role.STUDENT
          ? same(u.student, s)
          : u.role == Role.TEACHER
              && lessons.stream().anyMatch(l -> lesson(u, l) && same(l.group, s.group));
    if (row instanceof StudyGroup g)
      return u.role == Role.STAFF
          || u.role == Role.STUDENT && same(u.student.group, g)
          || u.role == Role.TEACHER
              && (same(g.curator, u.teacher)
                  || lessons.stream().anyMatch(l -> lesson(u, l) && same(g, l.group)));
    if (row instanceof Subject s)
      return u.role == Role.STAFF
          || u.role == Role.TEACHER && u.teacher.subjects.stream().anyMatch(x -> same(x, s))
          || u.role == Role.STUDENT && u.student.group.subjects.stream().anyMatch(x -> same(x, s));
    return row instanceof Room || row instanceof Semester;
  }

  public static boolean write(User u, String kind, RecordEntity row) {
    if (u.role == Role.ADMIN) return !kind.equals("audit");
    if (kind.equals("reviews"))
      return u.role == Role.STAFF
          && (row == null || row instanceof LessonReview r && same(r.reviewer, u));
    if (u.role != Role.TEACHER) return false;
    if (row == null) return kind.equals("grades") || kind.equals("attendance");
    Lesson l = row instanceof Grade g ? g.lesson : row instanceof Attendance a ? a.lesson : null;
    return l != null && same(u.teacher, l.teacher);
  }

  public static void deny() {
    throw new BusinessException(403, "Недостаточно прав для этой операции");
  }
}
