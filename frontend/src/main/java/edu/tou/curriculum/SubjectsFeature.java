package edu.tou.curriculum;

import edu.tou.shared.*;
import java.util.List;

public final class SubjectsFeature {
  private SubjectsFeature() {}

  public static final FeatureSchema SCHEMA =
      new FeatureSchema(
          "subjects",
          "Дисциплины",
          "Предметы и академические кредиты.",
          List.of(
              new FieldSpec("name", "Дисциплина", "text", "", true),
              new FieldSpec("code", "Код предмета", "text", "", true),
              new FieldSpec("credits", "Кредиты (1–30)", "number", "", true)),
          List.of(
              FeatureSchema.col("name", "Название"),
              FeatureSchema.col("code", "Код"),
              FeatureSchema.col("credits", "Кредиты"),
              FeatureSchema.col("teacherCount", "Преподавателей"),
              FeatureSchema.col("avg", "Средний балл")),
          "");
}
