package edu.tou.shared;

public class BusinessException extends RuntimeException {
  public final int status;

  public BusinessException(String message) {
    this(400, message);
  }

  public BusinessException(int status, String message) {
    super(message);
    this.status = status;
  }

  public static void require(boolean condition, String message) {
    if (!condition) throw new BusinessException(message);
  }
}
