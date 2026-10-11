package edu.tou.identity;

public enum Role {
  ADMIN,
  TEACHER,
  STUDENT,
  STAFF;

  public static Role parse(String value) {
    String normalized =
        java.util.Objects.toString(value, "").trim().toUpperCase(java.util.Locale.ROOT);
    return valueOf(normalized.equals("STUFF") ? "STAFF" : normalized);
  }
}
