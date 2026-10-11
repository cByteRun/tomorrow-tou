package edu.tou.shared;

import edu.tou.attendance.Attendance;
import edu.tou.audit.AuditEvent;
import edu.tou.curriculum.*;
import edu.tou.grades.Grade;
import edu.tou.groups.StudyGroup;
import edu.tou.identity.User;
import edu.tou.schedule.*;
import edu.tou.students.Student;
import edu.tou.supervision.LessonReview;
import edu.tou.teachers.Teacher;
import java.util.*;

public final class EntityCatalog {
  private EntityCatalog() {}

  public static final Map<String, Class<? extends RecordEntity>> TYPES =
      Map.ofEntries(
          Map.entry("users", User.class),
          Map.entry("students", Student.class),
          Map.entry("teachers", Teacher.class),
          Map.entry("groups", StudyGroup.class),
          Map.entry("subjects", Subject.class),
          Map.entry("semesters", Semester.class),
          Map.entry("rooms", Room.class),
          Map.entry("lessons", Lesson.class),
          Map.entry("attendance", Attendance.class),
          Map.entry("grades", Grade.class),
          Map.entry("reviews", LessonReview.class),
          Map.entry("audit", AuditEvent.class));

  public static Class<? extends RecordEntity> type(String key) {
    Class<? extends RecordEntity> t = TYPES.get(key);
    if (t == null) throw new BusinessException(404, "Раздел не найден");
    return t;
  }
}
