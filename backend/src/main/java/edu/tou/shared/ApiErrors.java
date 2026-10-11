package edu.tou.shared;

import java.sql.SQLException;
import java.util.Map;
import org.apache.logging.log4j.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiErrors {
  private final Logger log = LogManager.getLogger(getClass());

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<?> business(BusinessException e) {
    return ResponseEntity.status(e.status).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler({
    org.springframework.dao.DataIntegrityViolationException.class,
    jakarta.persistence.PersistenceException.class,
    org.springframework.orm.ObjectOptimisticLockingFailureException.class
  })
  ResponseEntity<?> conflict(Exception e) {
    log.warn("Constraint or concurrent update: {}", e.getClass().getSimpleName());
    String message = "Запись изменена другим пользователем. Обновите данные.";
    for (Throwable cause = e; cause != null; cause = cause.getCause())
      if (cause instanceof SQLException sql) {
        String detail = sql.getMessage();
        message =
            switch (sql.getSQLState()) {
              case "23P01" ->
                  "Конфликт расписания "
                      + (detail.contains("teacher_overlap")
                          ? "преподавателя"
                          : detail.contains("room_overlap") ? "аудитории" : "группы")
                      + ". Выберите другое время.";
              case "23503" ->
                  "Запись используется в связанных данных. Удаление или изменение связи"
                      + " невозможно.";
              case "23505" ->
                  detail.contains("email")
                      ? "Эта электронная почта уже используется."
                      : detail.contains("student_no")
                          ? "Номер студента уже используется."
                          : "Такая запись уже существует. Проверьте уникальные поля или обновите"
                              + " журнал.";
              case "23514" -> "Нарушено ограничение данных. Проверьте значения и связанные записи.";
              default -> "Операция противоречит ограничениям базы данных. Обновите данные.";
            };
        break;
      }
    return ResponseEntity.status(409).body(Map.of("message", message));
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.bind.MethodArgumentNotValidException.class
  })
  ResponseEntity<?> input(Exception e) {
    return ResponseEntity.badRequest()
        .body(Map.of("message", "Некорректные данные. Проверьте обязательные поля, даты и числа."));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unexpected(Exception e) {
    log.error("API failure", e);
    return ResponseEntity.internalServerError()
        .body(Map.of("message", "Не удалось выполнить операцию. Повторите позже."));
  }
}
