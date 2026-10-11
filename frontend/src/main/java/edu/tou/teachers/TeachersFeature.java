package edu.tou.teachers;

import edu.tou.shared.*;
import java.util.List;

public final class TeachersFeature {
  private TeachersFeature() {}

  public static final FeatureSchema SCHEMA =
      new FeatureSchema(
          "teachers",
          "Преподаватели",
          "Дисциплины, расписание и преподавательская нагрузка.",
          List.of(
              new FieldSpec("name", "Полное имя", "text", "", true),
              new FieldSpec("email", "Электронная почта", "text", "", true),
              new FieldSpec("department", "Кафедра", "text", "", true),
              new FieldSpec("subjectIds", "Преподаваемые дисциплины", "multi", "subjects", true)),
          List.of(
              FeatureSchema.col("name", "Преподаватель"),
              FeatureSchema.col("email", "Почта"),
              FeatureSchema.col("subjectNames", "Дисциплины"),
              FeatureSchema.col("load", "Занятий"),
              FeatureSchema.col("hours", "Часов")),
          "subjectIds");
}
