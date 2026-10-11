package edu.tou.schedule;

import edu.tou.shared.*;
import java.util.List;

public final class RoomsFeature {
  private RoomsFeature() {}

  public static final FeatureSchema SCHEMA =
      new FeatureSchema(
          "rooms",
          "Аудитории",
          "Помещения, корпуса и ограничения по вместимости.",
          List.of(
              new FieldSpec("name", "Номер аудитории", "text", "", true),
              new FieldSpec("building", "Корпус", "text", "", true),
              new FieldSpec("capacity", "Количество мест (1–1000)", "number", "", true)),
          List.of(
              FeatureSchema.col("name", "Аудитория"),
              FeatureSchema.col("building", "Корпус"),
              FeatureSchema.col("capacity", "Мест")),
          "");
}
