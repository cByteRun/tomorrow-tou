package edu.tou.shared;

public record FieldSpec(String key, String label, String type, String source, boolean required) {
  public static FieldSpec text(String key, String label) {
    return new FieldSpec(key, label, "text", "", true);
  }

  public static FieldSpec select(String key, String label, String source) {
    return new FieldSpec(key, label, "select", source, true);
  }
}
