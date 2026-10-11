package edu.tou.identity;

import edu.tou.shared.*;
import java.util.List;

public final class UsersFeature {
  private UsersFeature() {}

  public static final FeatureSchema SCHEMA =
      new FeatureSchema(
          "users",
          "Пользователи",
          "Учётные записи, четыре роли и привязка к карточкам.",
          List.of(
              new FieldSpec("name", "Полное имя", "text", "", true),
              new FieldSpec("email", "Логин / email", "text", "", true),
              new FieldSpec(
                  "password",
                  "Пароль (8–72 символа; пустой — без изменений)",
                  "password",
                  "",
                  true),
              new FieldSpec("role", "Роль", "select", "roles", true),
              new FieldSpec("entityId", "Связанная карточка", "select", "userEntities", false),
              new FieldSpec("enabled", "Аккаунт активен", "boolean", "", false)),
          List.of(
              FeatureSchema.col("name", "Имя"),
              FeatureSchema.col("email", "Логин"),
              FeatureSchema.col("role", "Роль"),
              FeatureSchema.col("enabled", "Активен")),
          "role");
}
