package edu.tou.grades;

import edu.tou.shared.*;
import edu.tou.workspace.*;
import java.util.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class GradesView extends VBox {
  private final Workspace ws;
  private final ComboBox<EntityDialog.Choice> lesson, type;
  private final VBox roster = new VBox(12);

  public GradesView(Workspace ws) {
    super(20);
    this.ws = ws;
    lesson = EntityDialog.select(ws, "lessons", null);
    type = EntityDialog.select(ws, "gradeTypes", "Текущая");
    lesson.setPrefWidth(700);
    lesson.valueProperty().addListener((o, a, b) -> render());
    type.valueProperty().addListener((o, a, b) -> render());
    getChildren()
        .addAll(
            Ui.label("Оценки и ведомости", "page-title"),
            Ui.label(
                "Средний балл: "
                    + Ui.str(ws.dashboard.get("average"))
                    + " · Все оценки имеют одинаковый вес.",
                "subtle"),
            new HBox(15, lesson, type),
            roster,
            new RecordsView(ws, GradesFeature.SCHEMA));
  }

  private void render() {
    roster.getChildren().clear();
    if (lesson.getValue() == null || type.getValue() == null) return;
    Map<String, Object> l =
        ws.rows("lessons").stream()
            .filter(x -> Objects.equals(x.get("id"), lesson.getValue().id()))
            .findFirst()
            .orElseThrow();
    boolean edit = ws.canWrite("grades") && !"cancelled".equals(l.get("status"));
    VBox card = Ui.card(Ui.label("Оценки занятия · " + l.get("subjectName"), "section-title"));
    for (Map<String, Object> s : ws.rows("students")) {
      if (!Objects.equals(s.get("groupId"), l.get("groupId"))) continue;
      Map<String, Object> grade =
          ws.rows("grades").stream()
              .filter(
                  g ->
                      Objects.equals(g.get("studentId"), s.get("id"))
                          && Objects.equals(g.get("lessonId"), l.get("id"))
                          && Objects.equals(g.get("type"), type.getValue().id()))
              .findFirst()
              .orElse(null);
      TextField score = new TextField(grade == null ? "" : grade.get("value").toString());
      score.setPromptText("0–100");
      score.setPrefWidth(100);
      score.setDisable(!edit);
      HBox row = new HBox(15, Ui.label(Ui.str(s.get("fullName")), "field-label"), score);
      Runnable save =
          () -> {
            try {
              int value = Integer.parseInt(score.getText().trim());
              if (value < 0 || value > 100) throw new NumberFormatException();
              Map<String, Object> input = new LinkedHashMap<>();
              input.put("studentId", s.get("id"));
              input.put("lessonId", l.get("id"));
              input.put("type", type.getValue().id());
              input.put("value", value);
              if (grade != null) input.put("version", grade.get("version"));
              ws.save("grades", grade, input, () -> {});
            } catch (NumberFormatException e) {
              Ui.notice(
                  "Проверьте оценку",
                  "Введите целое число от 0 до 100. Для удаления используйте кнопку «Удалить».");
            }
          };
      if (edit) {
        score.setOnAction(e -> save.run());
        row.getChildren().add(Ui.button("Сохранить", save, "btn-primary"));
        if (grade != null)
          row.getChildren()
              .add(Ui.button("Удалить", () -> ws.delete("grades", grade), "btn-danger"));
      }
      card.getChildren().add(row);
    }
    roster.getChildren().add(card);
  }
}
