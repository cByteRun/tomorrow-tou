package edu.tou.dashboard;

import edu.tou.shared.*;
import edu.tou.workspace.Workspace;
import java.util.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class DashboardView extends VBox {
  @SuppressWarnings("unchecked")
  public DashboardView(Workspace ws) {
    super(22);
    VBox hero =
        new VBox(
            15,
            Ui.label("◉  Академическое пространство", "chip"),
            Ui.label("Добро пожаловать, " + ws.user.get("name"), "hero-title"),
            Ui.label(
                ws.role().equals("STAFF")
                    ? "Контролируйте проведение занятий и нагрузку преподавателей."
                    : ws.teacher()
                        ? "Ваши дисциплины, занятия, посещаемость и результаты студентов."
                        : "Расписание, успеваемость и посещаемость — в одном пространстве.",
                "hero-desc"));
    hero.getStyleClass().add("hero");
    getChildren().add(hero);
    HBox metrics = new HBox(15);
    String[] labels = {
      ws.role().equals("STAFF") ? "Преподавателей" : "Студентов",
      "Дисциплин",
      "Занятий сегодня",
      ws.role().equals("STAFF") ? "Проверок" : "Средний балл"
    };
    Object[] values = {
      ws.dashboard.get(ws.role().equals("STAFF") ? "teachers" : "students"),
      ws.dashboard.get("subjects"),
      ws.dashboard.get("todayCount"),
      ws.dashboard.get(ws.role().equals("STAFF") ? "reviews" : "average")
    };
    for (int i = 0; i < 4; i++) {
      VBox card =
          Ui.card(
              Ui.label(labels[i], "muted-mini"),
              Ui.label(Ui.str(values[i]), "metric"),
              Ui.label("из базы данных", "muted-mini"));
      HBox.setHgrow(card, Priority.ALWAYS);
      card.setMaxWidth(Double.MAX_VALUE);
      metrics.getChildren().add(card);
    }
    getChildren().add(metrics);
    CategoryAxis x = new CategoryAxis();
    NumberAxis y = new NumberAxis();
    BarChart<String, Number> bars = new BarChart<>(x, y);
    bars.setLegendVisible(false);
    bars.setAnimated(false);
    bars.setPrefHeight(285);
    XYChart.Series<String, Number> series = new XYChart.Series<>();
    if (ws.role().equals("STAFF")) {
      for (Map<String, Object> t : ws.rows("teachers"))
        series.getData().add(new XYChart.Data<>(t.get("name").toString(), (Number) t.get("hours")));
    } else
      for (Map<String, Object> s :
          (List<Map<String, Object>>) ws.dashboard.getOrDefault("subjectChart", List.of()))
        if (s.get("average") instanceof Number n)
          series.getData().add(new XYChart.Data<>(s.get("name").toString(), n));
    bars.getData().add(series);
    PieChart pie = new PieChart();
    pie.setAnimated(false);
    pie.setPrefHeight(285);
    if (ws.role().equals("STAFF")) {
      for (String state : List.of("scheduled", "conducted", "cancelled"))
        pie.getData()
            .add(
                new PieChart.Data(
                    switch (state) {
                      case "scheduled" -> "Назначено";
                      case "conducted" -> "Проведено";
                      default -> "Отменено";
                    },
                    ws.rows("lessons").stream()
                        .filter(l -> state.equals(l.get("status")))
                        .count()));
    } else {
      Map<String, Object> marks =
          (Map<String, Object>) ws.dashboard.getOrDefault("attendance", Map.of());
      Map<String, String> names =
          Map.of(
              "present",
              "Присутствовал",
              "late",
              "Опоздал",
              "absent",
              "Отсутствовал",
              "excused",
              "Уважительная");
      marks.forEach(
          (k, v) -> {
            if (v instanceof Number n && n.doubleValue() > 0)
              pie.getData().add(new PieChart.Data(names.get(k), n.doubleValue()));
          });
    }
    VBox
        barCard =
            Ui.card(
                Ui.label(
                    ws.role().equals("STAFF")
                        ? "Нагрузка преподавателей, часы"
                        : "Средний балл по дисциплинам",
                    "section-title"),
                series.getData().isEmpty() ? Ui.label("Оценок пока нет", "subtle") : bars),
        pieCard =
            Ui.card(
                Ui.label(
                    ws.role().equals("STAFF") ? "Проведение занятий" : "Структура посещений",
                    "section-title"),
                pie.getData().isEmpty() ? Ui.label("Отметок пока нет", "subtle") : pie);
    HBox charts = new HBox(18, barCard, pieCard);
    HBox.setHgrow(barCard, Priority.ALWAYS);
    HBox.setHgrow(pieCard, Priority.ALWAYS);
    barCard.setMaxWidth(Double.MAX_VALUE);
    pieCard.setMaxWidth(Double.MAX_VALUE);
    getChildren().add(charts);
    VBox today = Ui.card(Ui.label("Занятия сегодня", "section-title"));
    List<Map<String, Object>> lessons =
        (List<Map<String, Object>>) ws.dashboard.getOrDefault("todayLessons", List.of());
    if (lessons.isEmpty()) today.getChildren().add(Ui.label("На сегодня занятий нет", "subtle"));
    for (Map<String, Object> l : lessons)
      today
          .getChildren()
          .add(
              new HBox(
                  20,
                  Ui.label(l.get("start") + "–" + l.get("end"), "timebox"),
                  new VBox(
                      5,
                      Ui.label(Ui.str(l.get("subjectName")), "section-title"),
                      Ui.label(
                          l.get("groupName")
                              + " · "
                              + l.get("teacherName")
                              + " · "
                              + l.get("roomName"),
                          "subtle"))));
    getChildren().add(today);
  }
}
