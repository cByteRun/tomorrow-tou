package edu.tou.audit;

import edu.tou.shared.*;
import java.util.List;

public final class AuditFeature {
  private AuditFeature() {}

  public static final FeatureSchema SCHEMA =
      new FeatureSchema(
          "audit",
          "Журнал аудита",
          "История входов, изменений и выгрузок из базы данных.",
          List.of(),
          List.of(
              FeatureSchema.col("at", "Время"),
              FeatureSchema.col("actor", "Пользователь"),
              FeatureSchema.col("action", "Действие"),
              FeatureSchema.col("entity", "Раздел"),
              FeatureSchema.col("description", "Подробности")),
          "");
}
