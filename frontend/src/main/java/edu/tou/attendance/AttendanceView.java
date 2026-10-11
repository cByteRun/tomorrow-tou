package edu.tou.attendance;

import edu.tou.shared.*;
import edu.tou.workspace.*;
import java.time.LocalDate;
import java.util.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class AttendanceView extends VBox {
  private final Workspace ws;
  private final ComboBox<EntityDialog.Choice> lesson;
  private final VBox roster = new VBox(12);

  public AttendanceView(Workspace ws) {
    super(20);
    this.ws = ws;
    lesson = EntityDialog.select(ws, "lessons", null);
    lesson.setPrefWidth(800);
    lesson.valueProperty().addListener((o, a, b) -> render());
    getChildren()
        .addAll(
            Ui.label("Посещаемость", "page-title"),
            Ui.label("Выберите занятие для просмотра и отметки посещаемости.", "subtle"),
            lesson,
            roster,
            new RecordsView(ws, AttendanceFeature.SCHEMA));
  }

  private void render() {
    roster.getChildren().clear();
    if (lesson.getValue() == null) return;
    Map<String, Object> l =
        ws.rows("lessons").stream()
            .filter(x -> Objects.equals(x.get("id"), lesson.getValue().id()))
            .findFirst()
            .orElseThrow();
    boolean edit =
        ws.canWrite("attendance")
            && !LocalDate.parse(l.get("date").toString()).isAfter(LocalDate.now())
            && !"cancelled".equals(l.get("status"));
    VBox card =
        Ui.card(
            Ui.label(
                "Ведомость · " + l.get("subjectName") + " · " + l.get("groupName"),
                "section-title"));
    if (edit)
      card.getChildren()
          .add(
              Ui.button(
                  "Отметить неотмеченных присутствующими",
                  () -> {
                    if (Ui.confirm(
                        "Массовая отметка",
                        "Поставить присутствие всем студентам без отметки? Существующие отметки"
                            + " сохраняются."))
                      ws.perform(
                          () ->
                              ws.api.request(
                                  "POST", "/records/attendance/mark-all/" + l.get("id"), null),
                          v -> ws.reload());
                  },
                  "btn-soft"));
    for (Map<String, Object> s : ws.rows("students")) {
      if (!Objects.equals(s.get("groupId"), l.get("groupId"))) continue;
      Map<String, Object> mark =
          ws.rows("attendance").stream()
              .filter(
                  a ->
                      Objects.equals(a.get("lessonId"), l.get("id"))
                          && Objects.equals(a.get("studentId"), s.get("id")))
              .findFirst()
              .orElse(null);
      ComboBox<EntityDialog.Choice> status =
          EntityDialog.select(ws, "attendanceStatuses", mark == null ? null : mark.get("status"));
      status.setDisable(!edit);
      HBox row = new HBox(15, Ui.label(Ui.str(s.get("fullName")), "field-label"), status);
      if (edit) {
        row.getChildren()
            .add(
                Ui.button(
                    "Сохранить",
                    () -> {
                      if (status.getValue() == null) {
                        Ui.notice("Отметка", "Выберите статус");
                        return;
                      }
                      Map<String, Object> input = new LinkedHashMap<>();
                      input.put("studentId", s.get("id"));
                      input.put("lessonId", l.get("id"));
                      input.put("status", status.getValue().id());
                      if (mark != null) input.put("version", mark.get("version"));
                      ws.save("attendance", mark, input, () -> {});
                    },
                    "btn-primary"));
        if (mark != null)
          row.getChildren()
              .add(Ui.button("Очистить", () -> ws.delete("attendance", mark), "btn-danger"));
      }
      card.getChildren().add(row);
    }
    roster.getChildren().add(card);
  }
}
