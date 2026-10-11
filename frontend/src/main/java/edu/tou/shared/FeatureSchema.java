package edu.tou.shared;

import java.util.List;

public record FeatureSchema(
    String key,
    String title,
    String description,
    List<FieldSpec> fields,
    List<Column> columns,
    String filterKey) {
  public record Column(String key, String label) {}

  public static Column col(String key, String label) {
    return new Column(key, label);
  }
}
