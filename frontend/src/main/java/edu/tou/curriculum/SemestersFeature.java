package edu.tou.curriculum;

import edu.tou.shared.*;
import java.util.List;

public final class SemestersFeature {
  private SemestersFeature() {}

  public static final FeatureSchema SCHEMA =
      new FeatureSchema(
          "semesters",
          "Семестры",
          "Границы учебных периодов и статусы.",
          List.of(
              new FieldSpec("name", "Название", "text", "", true),
              new FieldSpec("start", "Дата начала", "date", "", true),
              new FieldSpec("end", "Дата окончания", "date", "", true),
              new FieldSpec("status", "Статус", "select", "semesterStatuses", true)),
          List.of(
              FeatureSchema.col("name", "Название"),
              FeatureSchema.col("start", "Начало"),
              FeatureSchema.col("end", "Окончание"),
              FeatureSchema.col("status", "Статус")),
          "");
}
