package edu.tou.supervision;

import edu.tou.shared.*;
import java.util.List;

public final class ReviewsFeature {
  private ReviewsFeature() {}

  public static final FeatureSchema SCHEMA =
      new FeatureSchema(
          "reviews",
          "Контроль преподавателей",
          "Сотрудник проверяет проведение занятий и фиксирует замечания.",
          List.of(
              new FieldSpec("lessonId", "Занятие преподавателя", "select", "lessons", true),
              new FieldSpec("result", "Результат проверки", "select", "reviewResults", true),
              new FieldSpec("note", "Комментарий проверки", "text", "", true)),
          List.of(
              FeatureSchema.col("date", "Дата"),
              FeatureSchema.col("teacherName", "Преподаватель"),
              FeatureSchema.col("subjectName", "Дисциплина"),
              FeatureSchema.col("result", "Результат"),
              FeatureSchema.col("note", "Комментарий"),
              FeatureSchema.col("reviewerName", "Сотрудник")),
          "lessonId");
}
