package edu.tou.students;

import edu.tou.shared.*;
import java.util.List;

public final class StudentsFeature {
  private StudentsFeature() {}

  public static final FeatureSchema SCHEMA =
      new FeatureSchema(
          "students",
          "Студенты",
          "Реестр обучающихся, групп и контактной информации.",
          List.of(
              new FieldSpec("firstName", "Имя", "text", "", true),
              new FieldSpec("lastName", "Фамилия", "text", "", true),
              new FieldSpec("studentNo", "Номер студента", "text", "", true),
              new FieldSpec("groupId", "Учебная группа", "select", "groups", true),
              new FieldSpec("email", "Электронная почта", "text", "", true),
              new FieldSpec("phone", "Телефон", "optional", "", false)),
          List.of(
              FeatureSchema.col("fullName", "ФИО"),
              FeatureSchema.col("groupName", "Группа"),
              FeatureSchema.col("email", "Почта"),
              FeatureSchema.col("studentNo", "Номер"),
              FeatureSchema.col("avg", "Средний балл")),
          "groupId");
}
