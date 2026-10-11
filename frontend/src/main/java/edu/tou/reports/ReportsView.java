package edu.tou.reports;

import edu.tou.shared.*;
import edu.tou.workspace.*;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

public class ReportsView extends VBox {
  public ReportsView(Workspace ws) {
    super(20);
    ComboBox<EntityDialog.Choice> semester = EntityDialog.select(ws, "semesters", null),
        group = EntityDialog.select(ws, "groups", null),
        student =
            EntityDialog.select(
                ws, "students", ws.role().equals("STUDENT") ? ws.user.get("entityId") : null);
    semester.getItems().addFirst(new EntityDialog.Choice(null, "Все семестры"));
    semester.getSelectionModel().selectFirst();
    group.getItems().addFirst(new EntityDialog.Choice(null, "Все группы"));
    group.getSelectionModel().selectFirst();
    Label summary = Ui.label("", "subtle");
    Runnable update =
        () -> {
          List<Map<String, Object>> grades =
              ws.rows("grades").stream()
                  .filter(
                      g ->
                          (semester.getValue() == null
                                  || semester.getValue().id() == null
                                  || Objects.equals(g.get("semesterId"), semester.getValue().id()))
                              && (group.getValue() == null
                                  || group.getValue().id() == null
                                  || Objects.equals(g.get("groupId"), group.getValue().id())))
                  .toList();
          summary.setText(
              "Оценок в ведомости: "
                  + grades.size()
                  + " · Средний балл: "
                  + (grades.isEmpty()
                      ? "—"
                      : String.format(
                          Locale.ROOT,
                          "%.1f",
                          grades.stream()
                              .mapToInt(g -> ((Number) g.get("value")).intValue())
                              .average()
                              .orElseThrow())));
        };
    semester.valueProperty().addListener((o, a, b) -> update.run());
    group.valueProperty().addListener((o, a, b) -> update.run());
    update.run();
    getChildren()
        .addAll(
            Ui.label("Академические отчёты", "page-title"),
            Ui.label(
                "Документы формируются Apache POI из доступных вам записей PostgreSQL.", "subtle"),
            Ui.card(
                Ui.label("Ведомость успеваемости", "section-title"),
                Ui.label("Семестр", "field-label"),
                semester,
                Ui.label("Группа", "field-label"),
                group,
                summary,
                Ui.button(
                    "↓ Скачать Excel (.xlsx)",
                    () ->
                        download(
                            ws,
                            "grades.xlsx",
                            "/reports/grades.xlsx?"
                                + query("semesterId", semester)
                                + query("groupId", group)),
                    "btn-primary")),
            Ui.card(
                Ui.label("Академическая справка", "section-title"),
                Ui.label("Студент", "field-label"),
                student,
                Ui.label("Период справки соответствует выбранному выше семестру.", "subtle"),
                Ui.button(
                    "↓ Скачать Word (.docx)",
                    () -> {
                      if (student.getValue() == null) {
                        Ui.notice("Студент", "Выберите доступного студента");
                        return;
                      }
                      download(
                          ws,
                          "student.docx",
                          "/reports/student.docx?studentId="
                              + student.getValue().id()
                              + "&"
                              + query("semesterId", semester));
                    },
                    "btn-primary")));
  }

  private String query(String key, ComboBox<EntityDialog.Choice> c) {
    return c.getValue() == null || c.getValue().id() == null
        ? ""
        : key + "=" + c.getValue().id() + "&";
  }

  private void download(Workspace ws, String filename, String path) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Сохранить отчёт");
    chooser.setInitialFileName(filename);
    chooser
        .getExtensionFilters()
        .add(
            new FileChooser.ExtensionFilter(
                "Office document", "*." + filename.substring(filename.lastIndexOf('.') + 1)));
    File file = chooser.showSaveDialog(getScene().getWindow());
    if (file == null) return;
    ws.perform(
        () -> {
          byte[] bytes = ws.api.download("GET", path, null);
          Files.write(file.toPath(), bytes);
          return bytes.length;
        },
        n -> Ui.notice("Отчёт сохранён", file.getAbsolutePath() + "\nРазмер: " + n + " байт"));
  }
}
