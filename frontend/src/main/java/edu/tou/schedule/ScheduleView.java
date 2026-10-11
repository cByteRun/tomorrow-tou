package edu.tou.schedule;

import edu.tou.shared.*;
import edu.tou.workspace.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ScheduleView extends VBox {
  private final Workspace ws;
  private LocalDate monday =
      LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  private final VBox body = new VBox(18);
  private final Label period = Ui.label("", "subtle");
  private boolean calendar = true;
  private final ComboBox<EntityDialog.Choice> group;

  public ScheduleView(Workspace ws) {
    super(20);
    this.ws = ws;
    group = EntityDialog.select(ws, "groups", null);
    group.getItems().addFirst(new EntityDialog.Choice(null, "Все группы"));
    group.getSelectionModel().selectFirst();
    group.valueProperty().addListener((o, a, b) -> render());
    HBox toolbar =
        new HBox(
            9,
            Ui.button(
                "←",
                () -> {
                  monday = monday.minusWeeks(1);
                  render();
                },
                "btn-outline"),
            Ui.button(
                "Сегодня",
                () -> {
                  monday =
                      LocalDate.now()
                          .with(
                              java.time.temporal.TemporalAdjusters.previousOrSame(
                                  DayOfWeek.MONDAY));
                  render();
                },
                "btn-outline"),
            Ui.button(
                "→",
                () -> {
                  monday = monday.plusWeeks(1);
                  render();
                },
                "btn-outline"),
            period,
            group,
            Ui.button(
                "Календарь / таблица",
                () -> {
                  calendar = !calendar;
                  render();
                },
                "btn-soft"));
    if (ws.admin())
      toolbar
          .getChildren()
          .add(
              Ui.button(
                  "+ Занятие",
                  () ->
                      EntityDialog.open(
                          ws,
                          LessonsFeature.SCHEMA,
                          null,
                          Map.of(
                              "date",
                              LocalDate.now().toString(),
                              "start",
                              "09:00",
                              "end",
                              "10:30",
                              "status",
                              "scheduled")),
                  "btn-primary"));
    getChildren()
        .addAll(
            Ui.label("Расписание занятий", "page-title"),
            Ui.label(
                "Общее расписание группы автоматически доступно каждому её студенту.", "subtle"),
            toolbar,
            body);
    render();
  }

  private void render() {
    period.setText(monday + " — " + monday.plusDays(6));
    Object id = group.getValue() == null ? null : group.getValue().id();
    List<Map<String, Object>> lessons =
        ws.rows("lessons").stream()
            .filter(l -> (id == null || Objects.equals(l.get("groupId"), id)))
            .sorted(Comparator.comparing(l -> l.get("date").toString() + l.get("start")))
            .toList();
    body.getChildren().clear();
    if (!calendar) {
      body.getChildren().add(new RecordsView(ws, LessonsFeature.SCHEMA, lessons));
      return;
    }
    HBox week = new HBox(8);
    for (int day = 0; day < 7; day++) {
      LocalDate date = monday.plusDays(day);
      VBox column =
          Ui.card(
              Ui.label(
                  date.format(
                      DateTimeFormatter.ofPattern("EEE, dd.MM", Locale.forLanguageTag("ru"))),
                  "field-label"));
      column.setMinWidth(150);
      HBox.setHgrow(column, Priority.ALWAYS);
      for (Map<String, Object> l : lessons)
        if (l.get("date").equals(date.toString())) {
          Button event =
              Ui.button(
                  l.get("start")
                      + "–"
                      + l.get("end")
                      + "\n"
                      + l.get("subjectName")
                      + "\n"
                      + l.get("groupName")
                      + " · "
                      + l.get("roomName")
                      + "\n"
                      + l.get("teacherName")
                      + "\n"
                      + status(l),
                  () -> {
                    if (ws.admin()) EntityDialog.open(ws, LessonsFeature.SCHEMA, l);
                    else
                      Ui.notice(
                          "Занятие",
                          l.get("subjectName")
                              + "\n"
                              + l.get("topic")
                              + "\n"
                              + l.get("teacherName"));
                  },
                  "calendar-event");
          event.setWrapText(true);
          event.setMaxWidth(Double.MAX_VALUE);
          if ("cancelled".equals(l.get("status"))) event.getStyleClass().add("cancelled");
          column.getChildren().add(event);
        }
      week.getChildren().add(column);
    }
    ScrollPane scroll = new ScrollPane(week);
    scroll.setFitToHeight(true);
    scroll.setPrefViewportHeight(430);
    body.getChildren().add(scroll);
  }

  private String status(Map<String, Object> l) {
    return switch (l.get("status").toString()) {
      case "conducted" -> "✓ Проведено";
      case "cancelled" -> "Отменено";
      default -> "По расписанию";
    };
  }
}
