package edu.tou.attendance;

import edu.tou.shared.*;
import java.util.List;

public final class AttendanceFeature {
  private AttendanceFeature() {}

  public static final FeatureSchema SCHEMA =
      new FeatureSchema(
          "attendance",
          "Посещаемость",
          "Ведомость посещаемости. Будущие занятия недоступны для отметок.",
          List.of(
              new FieldSpec("studentId", "Студент", "select", "students", true),
              new FieldSpec("lessonId", "Занятие", "select", "lessons", true),
              new FieldSpec("status", "Отметка", "select", "attendanceStatuses", true)),
          List.of(
              FeatureSchema.col("studentName", "Студент"),
              FeatureSchema.col("subjectName", "Дисциплина"),
              FeatureSchema.col("date", "Дата"),
              FeatureSchema.col("status", "Отметка"),
              FeatureSchema.col("updatedAt", "Обновлено")),
          "lessonId");
}
