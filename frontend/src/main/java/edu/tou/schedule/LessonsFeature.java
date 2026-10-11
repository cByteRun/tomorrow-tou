package edu.tou.schedule;

import edu.tou.shared.*;
import java.util.List;

public final class LessonsFeature {
  private LessonsFeature() {}

  public static final FeatureSchema SCHEMA =
      new FeatureSchema(
          "lessons",
          "Расписание занятий",
          "Пересечения преподавателя, группы и аудитории проверяет API.",
          List.of(
              new FieldSpec("date", "Дата", "date", "", true),
              new FieldSpec("start", "Начало (ЧЧ:ММ)", "text", "", true),
              new FieldSpec("end", "Окончание (ЧЧ:ММ)", "text", "", true),
              new FieldSpec("semesterId", "Семестр", "select", "semesters", true),
              new FieldSpec("groupId", "Группа", "select", "groups", true),
              new FieldSpec("subjectId", "Дисциплина", "select", "subjects", true),
              new FieldSpec("teacherId", "Преподаватель", "select", "teachers", true),
              new FieldSpec("roomId", "Аудитория", "select", "rooms", true),
              new FieldSpec("topic", "Тема / тип занятия", "optional", "", false),
              new FieldSpec("status", "Статус", "select", "lessonStatuses", true)),
          List.of(
              FeatureSchema.col("date", "Дата"),
              FeatureSchema.col("start", "Начало"),
              FeatureSchema.col("end", "Конец"),
              FeatureSchema.col("subjectName", "Дисциплина"),
              FeatureSchema.col("groupName", "Группа"),
              FeatureSchema.col("teacherName", "Преподаватель"),
              FeatureSchema.col("roomName", "Аудитория"),
              FeatureSchema.col("status", "Статус")),
          "groupId");
}
