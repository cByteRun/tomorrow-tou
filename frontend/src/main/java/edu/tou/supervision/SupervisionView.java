package edu.tou.supervision;

import edu.tou.shared.*;
import edu.tou.workspace.*;
import java.util.*;
import javafx.scene.layout.*;

public class SupervisionView extends VBox {
  public SupervisionView(Workspace ws) {
    super(20);
    getChildren()
        .addAll(
            Ui.label("Контроль преподавателей", "page-title"),
            Ui.label(
                "Проведение занятий, преподавательская нагрузка и результаты проверок сотрудника.",
                "subtle"));
    HBox stats = new HBox(15);
    for (String state : List.of("scheduled", "conducted", "cancelled")) {
      String label =
          switch (state) {
            case "scheduled" -> "Назначено";
            case "conducted" -> "Проведено";
            default -> "Отменено";
          };
      stats
          .getChildren()
          .add(
              Ui.card(
                  Ui.label(label, "muted-mini"),
                  Ui.label(
                      ""
                          + ws.rows("lessons").stream()
                              .filter(l -> state.equals(l.get("status")))
                              .count(),
                      "metric")));
    }
    getChildren().add(stats);
    getChildren().add(new RecordsView(ws, edu.tou.teachers.TeachersFeature.SCHEMA));
    getChildren().add(new RecordsView(ws, ReviewsFeature.SCHEMA));
  }
}
