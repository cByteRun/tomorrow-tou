package edu.tou.shared;

import java.util.concurrent.*;
import java.util.function.*;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class Ui {
  private Ui() {}

  private static final ExecutorService IO =
      Executors.newFixedThreadPool(
          4,
          r -> {
            Thread t = new Thread(r, "tou-api");
            t.setDaemon(true);
            return t;
          });

  public static Label label(String text, String css) {
    Label l = new Label(text);
    l.getStyleClass().add(css);
    l.setWrapText(true);
    return l;
  }

  public static Button button(String text, Runnable action, String... styles) {
    Button b = new Button(text);
    b.getStyleClass().addAll(styles);
    b.setOnAction(e -> action.run());
    return b;
  }

  public static VBox card(Node... children) {
    VBox box = new VBox(14, children);
    box.getStyleClass().add("surface");
    box.setPadding(new Insets(24));
    return box;
  }

  public static <T> void async(Callable<T> job, Consumer<T> done) {
    async(job, done, Ui::error);
  }

  public static <T> void async(Callable<T> job, Consumer<T> done, Consumer<Throwable> failed) {
    Task<T> t =
        new Task<>() {
          protected T call() throws Exception {
            return job.call();
          }
        };
    t.setOnSucceeded(e -> done.accept(t.getValue()));
    t.setOnFailed(e -> failed.accept(t.getException()));
    IO.submit(t);
  }

  public static void error(Throwable e) {
    String text =
        e instanceof java.net.ConnectException || e instanceof java.net.http.HttpTimeoutException
            ? "API недоступен. Запустите backend и проверьте адрес сервера."
            : e.getMessage();
    Alert alert =
        new Alert(
            Alert.AlertType.ERROR,
            text == null ? "Не удалось выполнить действие" : text,
            ButtonType.OK);
    alert.setHeaderText("Операция не выполнена");
    alert.showAndWait();
  }

  public static void notice(String title, String message) {
    Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
    a.setHeaderText(title);
    a.showAndWait();
  }

  public static boolean confirm(String title, String message) {
    Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.CANCEL, ButtonType.OK);
    a.setHeaderText(title);
    return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
  }

  public static String str(Object value) {
    return value == null
        ? "—"
        : value instanceof java.util.Collection<?> c
            ? String.join(", ", c.stream().map(Object::toString).toList())
            : value.toString();
  }

  public static String role(String r) {
    return switch (r) {
      case "ADMIN" -> "Администратор";
      case "TEACHER" -> "Преподаватель";
      case "STUDENT" -> "Студент";
      case "STAFF" -> "Сотрудник";
      default -> r;
    };
  }
}
