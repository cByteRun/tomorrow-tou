package edu.tou.workspace;

import edu.tou.attendance.AttendanceView;
import edu.tou.dashboard.DashboardView;
import edu.tou.grades.GradesView;
import edu.tou.profile.ProfileView;
import edu.tou.reports.ReportsView;
import edu.tou.schedule.ScheduleView;
import edu.tou.shared.*;
import edu.tou.supervision.SupervisionView;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class Workspace extends BorderPane {
  public final ApiClient api;
  public Map<String, Object> user;
  private final Runnable exited;
  private final VBox content = new VBox(20);
  private final Label routeLabel = Ui.label("Обзор", "top-route"), status = Ui.label("", "subtle");
  private final Map<String, Button> navigation = new LinkedHashMap<>();
  public final Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
  public Map<String, Object> dashboard = new LinkedHashMap<>();
  private String page = "dashboard";
  private long generation;

  public Workspace(ApiClient api, Map<String, Object> user, Runnable exited) {
    this.api = api;
    this.user = user;
    this.exited = exited;
    getStyleClass().add("workspace");
    VBox sidebar = new VBox(5);
    sidebar.getStyleClass().add("sidebar");
    sidebar.setPrefWidth(248);
    sidebar.setPadding(new Insets(26, 15, 20, 15));
    sidebar
        .getChildren()
        .addAll(
            Ui.label("◈  TomorrowTOU", "brand"),
            Ui.label("UNIVERSITY EXPERIENCE", "eyebrow"),
            new Separator(),
            Ui.label("УЧЕБНОЕ ПРОСТРАНСТВО", "nav-caption"));
    nav(sidebar, "dashboard", "◫  Обзор");
    if (!role().equals("STAFF")) nav(sidebar, "students", "♙  Студенты");
    nav(sidebar, "teachers", "♧  Преподаватели");
    nav(sidebar, "groups", "◇  Учебные группы");
    nav(sidebar, "schedule", "▦  Расписание");
    if (!role().equals("STAFF")) {
      nav(sidebar, "attendance", "✓  Посещаемость");
      nav(sidebar, "grades", "☆  Оценки");
      nav(sidebar, "reports", "▤  Отчёты");
    }
    if (admin() || role().equals("STAFF"))
      nav(sidebar, "supervision", "◎  Контроль преподавателей");
    nav(sidebar, "profile", "⚙  Профиль");
    if (admin()) {
      sidebar.getChildren().add(Ui.label("АДМИНИСТРИРОВАНИЕ", "nav-caption"));
      nav(sidebar, "subjects", "▣  Дисциплины");
      nav(sidebar, "rooms", "▥  Аудитории");
      nav(sidebar, "semesters", "◷  Семестры");
      nav(sidebar, "users", "♙  Пользователи");
      nav(sidebar, "audit", "↝  Журнал аудита");
      nav(sidebar, "quality", "☑  Контроль качества");
    }
    Region space = new Region();
    VBox.setVgrow(space, Priority.ALWAYS);
    sidebar
        .getChildren()
        .addAll(
            space,
            Ui.card(
                Ui.label(user.get("name").toString(), "user-name"),
                Ui.label(Ui.role(role()), "subtle"),
                Ui.button("Выйти ↗", this::logout, "btn-outline")));
    ScrollPane sideScroll = new ScrollPane(sidebar);
    sideScroll.setFitToWidth(true);
    sideScroll.setPrefWidth(248);
    sideScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    sideScroll.getStyleClass().add("side-scroll");
    setLeft(sideScroll);
    HBox top = new HBox(18, routeLabel);
    top.setAlignment(Pos.CENTER_LEFT);
    top.getStyleClass().add("topbar");
    Region fill = new Region();
    HBox.setHgrow(fill, Priority.ALWAYS);
    top.getChildren()
        .addAll(
            fill,
            Ui.label(java.time.LocalDate.now().toString(), "subtle"),
            Ui.button("↻ Обновить", this::reload, "btn-outline"));
    content.setPadding(new Insets(30, 37, 40, 37));
    ScrollPane scroll = new ScrollPane(content);
    scroll.setFitToWidth(true);
    scroll.getStyleClass().add("content-scroll");
    BorderPane main = new BorderPane(scroll, top, null, status, null);
    setCenter(main);
    reload();
  }

  public String role() {
    return user.get("role").toString();
  }

  public boolean admin() {
    return role().equals("ADMIN");
  }

  public boolean teacher() {
    return role().equals("TEACHER");
  }

  public boolean canWrite(String kind) {
    return admin() && !kind.equals("audit")
        || teacher() && (kind.equals("grades") || kind.equals("attendance"))
        || role().equals("STAFF") && kind.equals("reviews");
  }

  public boolean canEdit(String kind, Map<String, Object> row) {
    return canWrite(kind)
        && (!kind.equals("reviews")
            || admin()
            || Objects.equals(row.get("reviewerId"), user.get("id")));
  }

  public boolean canDelete(String kind, Map<String, Object> row) {
    return canEdit(kind, row)
        && !(kind.equals("users") && Objects.equals(row.get("id"), user.get("id")));
  }

  private void nav(VBox side, String key, String text) {
    Button b = Ui.button(text, () -> navigate(key), "nav-item");
    b.setMaxWidth(Double.MAX_VALUE);
    navigation.put(key, b);
    side.getChildren().add(b);
  }

  public void navigate(String route) {
    if (!navigation.containsKey(route)) return;
    page = route;
    render();
  }

  public List<Map<String, Object>> rows(String kind) {
    return data.getOrDefault(kind, List.of());
  }

  public String name(String kind, Object id) {
    return rows(kind).stream()
        .filter(r -> Objects.equals(r.get("id"), id))
        .map(
            r ->
                Ui.str(
                    r.getOrDefault(
                        "name", r.getOrDefault("fullName", r.getOrDefault("topic", r.get("id"))))))
        .findFirst()
        .orElse("—");
  }

  public void reload() {
    long revision = ++generation;
    status.setText("Загрузка данных из API…");
    content.getChildren().setAll(new ProgressIndicator());
    Ui.async(
        () -> {
          Map<String, List<Map<String, Object>>> snapshot = new LinkedHashMap<>();
          for (String kind :
              List.of(
                  "students",
                  "teachers",
                  "groups",
                  "subjects",
                  "rooms",
                  "semesters",
                  "users",
                  "lessons",
                  "attendance",
                  "grades",
                  "reviews",
                  "audit")) snapshot.put(kind, api.rows(kind));
          return new Snapshot(snapshot, api.object("/dashboard"));
        },
        s -> {
          if (revision != generation) return;
          data.clear();
          data.putAll(s.data());
          dashboard = s.dashboard();
          status.setText("Данные из PostgreSQL · " + api.base());
          render();
        },
        e -> {
          if (revision != generation) return;
          status.setText("Ошибка загрузки");
          content
              .getChildren()
              .setAll(
                  Ui.card(
                      Ui.label("Не удалось загрузить данные", "section-title"),
                      Ui.label(Objects.toString(e.getMessage(), "API недоступен"), "subtle"),
                      Ui.button("Повторить", this::reload, "btn-primary")));
          failure(e);
        });
  }

  private record Snapshot(
      Map<String, List<Map<String, Object>>> data, Map<String, Object> dashboard) {}

  public <T> void perform(Callable<T> operation, Consumer<T> result) {
    setDisable(true);
    Ui.async(
        operation,
        value -> {
          setDisable(false);
          result.accept(value);
        },
        e -> {
          setDisable(false);
          failure(e);
        });
  }

  private void failure(Throwable e) {
    if (e instanceof ApiClient.ApiException a && a.status == 401) {
      api.token("");
      generation++;
      exited.run();
      Ui.notice("Сеанс завершён", "Войдите в систему повторно.");
    } else Ui.error(e);
  }

  public void save(
      String kind, Map<String, Object> original, Map<String, Object> values, Runnable done) {
    boolean edit = original != null;
    perform(
        () ->
            api.request(
                edit ? "PUT" : "POST",
                "/records/" + kind + (edit ? "/" + original.get("id") : ""),
                values),
        v -> {
          done.run();
          reload();
        });
  }

  public void delete(String kind, Map<String, Object> row) {
    if (!Ui.confirm(
        "Удалить запись?",
        "Действие удалит выбранную запись. Связанные с историей записи защищены от удаления."))
      return;
    perform(
        () ->
            api.request(
                "DELETE",
                "/records/" + kind + "/" + row.get("id") + "?version=" + row.get("version"),
                null),
        v -> reload());
  }

  private void logout() {
    perform(
        () -> api.request("POST", "/auth/logout", null),
        v -> {
          generation++;
          api.token("");
          exited.run();
        });
  }

  private void render() {
    routeLabel.setText(navigation.get(page).getText());
    navigation.forEach(
        (k, b) -> {
          b.getStyleClass().remove("active");
          if (k.equals(page)) b.getStyleClass().add("active");
        });
    Node view =
        switch (page) {
          case "dashboard" -> new DashboardView(this);
          case "schedule" -> new ScheduleView(this);
          case "attendance" -> new AttendanceView(this);
          case "grades" -> new GradesView(this);
          case "reports" -> new ReportsView(this);
          case "profile" -> new ProfileView(this);
          case "supervision" -> new SupervisionView(this);
          case "quality" -> quality();
          default -> new RecordsView(this, schema(page));
        };
    content.getChildren().setAll(view);
  }

  public static FeatureSchema schema(String page) {
    return switch (page) {
      case "students" -> edu.tou.students.StudentsFeature.SCHEMA;
      case "teachers" -> edu.tou.teachers.TeachersFeature.SCHEMA;
      case "groups" -> edu.tou.groups.GroupsFeature.SCHEMA;
      case "subjects" -> edu.tou.curriculum.SubjectsFeature.SCHEMA;
      case "rooms" -> edu.tou.schedule.RoomsFeature.SCHEMA;
      case "semesters" -> edu.tou.curriculum.SemestersFeature.SCHEMA;
      case "users" -> edu.tou.identity.UsersFeature.SCHEMA;
      case "audit" -> edu.tou.audit.AuditFeature.SCHEMA;
      default -> throw new IllegalArgumentException(page);
    };
  }

  private Node quality() {
    return Ui.card(
        Ui.label("Контроль качества", "page-title"),
        Ui.label("Проверки согласованности текущих данных из API", "subtle"),
        Ui.label(
            "Занятий: "
                + rows("lessons").size()
                + " · Оценок: "
                + rows("grades").size()
                + " · Отметок: "
                + rows("attendance").size(),
            "section-title"),
        Ui.label(
            "✓ Оценки 0–100: "
                + rows("grades").stream()
                    .allMatch(
                        g -> {
                          int n = ((Number) g.get("value")).intValue();
                          return n >= 0 && n <= 100;
                        }),
            "subtle"),
        Ui.label(
            "✓ Роли: "
                + rows("users").stream()
                    .allMatch(
                        u ->
                            List.of("ADMIN", "TEACHER", "STUDENT", "STAFF")
                                .contains(u.get("role"))),
            "subtle"),
        Ui.label(
            "Проверки прав, расписания и валидации входят в JUnit. Запуск: ./scripts/test.sh. Этот"
                + " экран показывает данные, а результаты тестов формирует Maven.",
            "subtle"));
  }
}
