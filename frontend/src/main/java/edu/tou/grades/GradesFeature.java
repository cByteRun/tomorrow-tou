package edu.tou.grades;

import edu.tou.shared.*;
import java.util.List;

public final class GradesFeature {
  private GradesFeature() {}

  public static final FeatureSchema SCHEMA =
      new FeatureSchema(
          "grades",
          "Оценки и ведомости",
          "Шкала 0–100. Среднее арифметическое по сохранённым оценкам.",
          List.of(
              new FieldSpec("studentId", "Студент", "select", "students", true),
              new FieldSpec("lessonId", "Занятие", "select", "lessons", true),
              new FieldSpec("type", "Вид оценки", "select", "gradeTypes", true),
              new FieldSpec("value", "Балл (0–100)", "number", "", true)),
          List.of(
              FeatureSchema.col("studentName", "Студент"),
              FeatureSchema.col("subjectName", "Дисциплина"),
              FeatureSchema.col("date", "Дата"),
              FeatureSchema.col("type", "Тип"),
              FeatureSchema.col("value", "Балл")),
          "lessonId");
}
