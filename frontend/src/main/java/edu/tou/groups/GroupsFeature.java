package edu.tou.groups;

import edu.tou.shared.*;
import java.util.List;

public final class GroupsFeature {
  private GroupsFeature() {}

  public static final FeatureSchema SCHEMA =
      new FeatureSchema(
          "groups",
          "Учебные группы",
          "Состав студентов, кураторы и программа обучения.",
          List.of(
              new FieldSpec("name", "Название группы", "text", "", true),
              new FieldSpec("year", "Курс (1–6)", "number", "", true),
              new FieldSpec("curatorId", "Куратор", "select", "teachers", true),
              new FieldSpec("subjectIds", "Дисциплины учебного плана", "multi", "subjects", true)),
          List.of(
              FeatureSchema.col("name", "Группа"),
              FeatureSchema.col("year", "Курс"),
              FeatureSchema.col("curatorName", "Куратор"),
              FeatureSchema.col("studentCount", "Студентов"),
              FeatureSchema.col("subjectNames", "Дисциплины"),
              FeatureSchema.col("avg", "Средний балл")),
          "id");
}
