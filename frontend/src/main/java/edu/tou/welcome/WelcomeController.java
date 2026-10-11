package edu.tou.welcome;

import edu.tou.shared.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class WelcomeController {
  @FXML private SplitPane split;
  @FXML private ScrollPane storyScroll;
  @FXML private VBox story, rail;
  @FXML private FlowPane roles;
  @FXML private Label caption, error, progress;
  @FXML private TextField email, plainPassword;
  @FXML private PasswordField password;
  @FXML private CheckBox remember;
  @FXML private Button login, showPassword, demoButton;
  private ApiClient api;
  private Consumer<Map<String, Object>> entered;
  private String role = "STUDENT";
  private final ToggleGroup roleGroup = new ToggleGroup();
  private final List<Label> phases = new ArrayList<>();
  private final List<VBox> chapters = new ArrayList<>();
  private final List<Canvas> scenes = new ArrayList<>();
  private Label explanation;
  private Timeline animation;
  private AnimationTimer frames;
  private double motionSeconds;
  private long previousFrame;
  private Timeline smoothScroll, railTransition;
  private final javafx.beans.property.ObjectProperty<Color> railColor =
      new javafx.beans.property.SimpleObjectProperty<>(Color.web("#e9ede1"));
  private int chapterIndex;
  private boolean paused;
  private int phase, student;
  private final AttendanceNarrative narrative = new AttendanceNarrative();
  private double phaseStarted;
  private final boolean reducedMotion = Boolean.getBoolean("tou.reducedMotion");
  private static final String[] PEOPLE = {
    "Данияр Ткачёв",
    "Алина Садыкова",
    "Руслан Омаров",
    "Мадина Жаксылыкова",
    "Арман Ибраев",
    "Аружан Касымова"
  };
  private static final String[] PHASES = {
    "Предмет", "Лицо преподавателя", "QR-сессия", "Лицо студента", "Bluetooth рядом", "В журнале"
  };
  private static final String[] COPY = {
    "Преподаватель выбирает свою дисциплину из списка и запускает занятие.",
    "Преподаватель подтверждает личность. Платформа подготавливает сессию выбранного предмета.",
    "Платформа выдаёт QR выбранной дисциплины, связанный с активной сессией преподавателя.",
    "Студент открывает QR в своём аккаунте и подтверждает личность сканом лица.",
    "Телефон преподавателя передаёт Bluetooth-сигнал. Клиент студента проверяет его доступность"
        + " рядом.",
    "Личность и близость подтверждены: отметка студента автоматически появляется в журнале занятия."
  };

  public void configure(ApiClient a, Consumer<Map<String, Object>> e) {
    api = a;
    entered = e;
  }

  @FXML
  private void initialize() {
    plainPassword.textProperty().bindBidirectional(password.textProperty());
    for (String r : List.of("STUDENT", "TEACHER", "STAFF", "ADMIN")) {
      ToggleButton b = new ToggleButton(Ui.role(r));
      b.setToggleGroup(roleGroup);
      b.setUserData(r);
      b.setOnAction(
          event -> {
            if (!b.isSelected()) b.setSelected(true);
          });
      roles.getChildren().add(b);
      if (r.equals(role)) b.setSelected(true);
    }
    roleGroup
        .selectedToggleProperty()
        .addListener(
            (o, old, v) -> {
              if (v == null) return;
              role = v.getUserData().toString();
              caption.setText(
                  switch (role) {
                    case "TEACHER" -> "Ваши группы, дисциплины и журнал занятий.";
                    case "STAFF" -> "Контроль преподавателей и учебного процесса.";
                    case "ADMIN" -> "Управление университетом, данными и доступом.";
                    default -> "Расписание, занятия и ваши отметки — рядом.";
                  });
              login.setText("Войти · " + Ui.role(role) + "                         ↗");
            });
    buildStory();
    split.widthProperty().addListener((o, old, width) -> resizeTypography());
    split
        .getDividers()
        .getFirst()
        .positionProperty()
        .addListener((o, old, value) -> resizeTypography());

    rail.minHeightProperty().bind(split.heightProperty().subtract(2));
    split
        .heightProperty()
        .addListener(
            (o, x, y) -> {
              for (VBox c : chapters) c.setMinHeight(Math.max(800, y.doubleValue()));
            });
    split
        .getDividers()
        .getFirst()
        .positionProperty()
        .addListener(
            (o, x, y) -> {
              if (y.doubleValue() < .45) split.setDividerPositions(.45);
              else if (y.doubleValue() > .70) split.setDividerPositions(.70);
            });
    railColor.addListener(
        (o, old, color) ->
            rail.setBackground(
                new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY))));
    storyScroll
        .vvalueProperty()
        .addListener(
            (o, x, y) -> {
              int index = Math.min(2, (int) Math.round(y.doubleValue() * 2));
              progress.setText(index == 0 ? "━ ─ ─" : index == 1 ? "─ ━ ─" : "─ ─ ━");
              if (index != chapterIndex) {
                chapterIndex = index;
                Color target =
                    Color.web(index == 1 ? "#e0e9e7" : index == 2 ? "#eae5db" : "#e9ede1");
                if (railTransition != null) railTransition.stop();
                railTransition =
                    new Timeline(
                        new KeyFrame(
                            Duration.seconds(reducedMotion ? 0 : 1),
                            new KeyValue(railColor, target, Interpolator.EASE_BOTH)));
                railTransition.play();
              }
            });
    Preferences prefs = Preferences.userNodeForPackage(getClass());
    email.setText(prefs.get("email", ""));
    String savedRole = prefs.get("role", "STUDENT");
    for (Toggle t : roleGroup.getToggles())
      if (t.getUserData().equals(savedRole)) t.setSelected(true);
    remember.setSelected(!email.getText().isBlank());
    animation =
        new Timeline(
            new KeyFrame(
                Duration.seconds(2.4),
                event -> {
                  if (motionActive()) {
                    narrative.advance();
                    phase = narrative.phase();
                    student = narrative.student();
                    phaseStarted = motionSeconds;
                    paint();
                  }
                }));
    animation.setCycleCount(Animation.INDEFINITE);
    if (reducedMotion) {
      narrative.complete();
      phase = narrative.phase();
      student = narrative.student();
    } else animation.play();
    frames =
        new AnimationTimer() {
          public void handle(long now) {
            if (previousFrame != 0 && motionActive())
              motionSeconds += Math.min(.05, (now - previousFrame) / 1_000_000_000.0);
            previousFrame = now;
            if (motionActive()) for (int i = 0; i < scenes.size(); i++) draw(scenes.get(i), i);
          }
        };
    frames.start();
    storyScroll.addEventFilter(
        javafx.scene.input.ScrollEvent.SCROLL,
        event -> {
          double extent = story.getHeight() - storyScroll.getViewportBounds().getHeight();
          if (extent <= 0 || event.getDeltaY() == 0) return;
          double destination =
              Math.max(0, Math.min(1, storyScroll.getVvalue() - event.getDeltaY() / extent));
          if (smoothScroll != null) smoothScroll.stop();
          smoothScroll =
              new Timeline(
                  new KeyFrame(
                      Duration.millis(240),
                      new KeyValue(
                          storyScroll.vvalueProperty(), destination, Interpolator.EASE_BOTH)));
          smoothScroll.play();
          event.consume();
        });
    paint();
  }

  private boolean motionActive() {
    if (paused || reducedMotion || split.getScene() == null || split.getScene().getWindow() == null)
      return false;
    var window = split.getScene().getWindow();
    return window.isShowing()
        && (!(window instanceof javafx.stage.Stage stage) || !stage.isIconified());
  }

  private void resizeTypography() {
    double size =
        split.getDividers().getFirst().getPosition() < .55
            ? 45
            : Math.max(42, Math.min(72, split.getWidth() * .044));
    for (VBox chapter : chapters)
      for (javafx.scene.Node node : chapter.getChildren())
        if (node.getStyleClass().contains("headline")) node.setStyle("-fx-font-size: " + size);
    double padding = Math.max(26, Math.min(72, split.getWidth() * .04));
    rail.setPadding(new Insets(35, padding, 25, padding));
  }

  public void dispose() {
    animation.stop();
    if (frames != null) frames.stop();
    if (smoothScroll != null) smoothScroll.stop();
    if (railTransition != null) railTransition.stop();
    password.clear();
  }

  private VBox chapter(String css) {
    VBox c = new VBox(13);
    c.getStyleClass().addAll("chapter", css);
    c.setMinHeight(900);
    c.setAlignment(Pos.CENTER_LEFT);
    chapters.add(c);
    story.getChildren().add(c);
    return c;
  }

  private void buildStory() {
    VBox first = chapter("first");
    HBox brand = new HBox(10, Ui.label("▦", "brand-icon"), Ui.label("TomorrowTOU", "brand"));
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    brand.getChildren().addAll(spacer, Ui.label("University control / Войти ↗", "mono"));
    first
        .getChildren()
        .addAll(
            brand,
            grow(),
            Ui.label("● 01 / ПОСЕЩАЕМОСТЬ НА АВТОПИЛОТЕ", "kicker"),
            Ui.label("Личность подтверждена.", "headline"),
            accent("Присутствие — тоже."),
            Ui.label(
                "Предмет, QR-сессия, подтверждение лица и Bluetooth рядом. От первого действия"
                    + " преподавателя до отметки студента — один автоматический процесс.",
                "lede"));
    FlowPane steps = new FlowPane(7, 7);
    for (int i = 0; i < PHASES.length; i++) {
      Label l = Ui.label("0" + (i + 1) + " " + PHASES[i], "phase");
      phases.add(l);
      steps.getChildren().add(l);
    }
    explanation = Ui.label("", "explanation");
    first.getChildren().addAll(steps, explanation, scene(first, 0));
    Button pause = Ui.button("Ⅱ Приостановить анимацию", () -> {}, "pause");
    pause.setOnAction(
        e -> {
          paused = !paused;
          pause.setText(paused ? "▷ Продолжить анимацию" : "Ⅱ Приостановить анимацию");
        });
    first
        .getChildren()
        .addAll(
            grow(),
            new Separator(),
            new HBox(18, Ui.label("Листайте — процесс продолжается ↓", "mono"), pause),
            Ui.label("Иллюстрация · не реальные отметки", "mono"));
    VBox second = chapter("secondary");
    second
        .getChildren()
        .addAll(
            grow(),
            Ui.label("● 02 / УНИВЕРСИТЕТ В ПРЯМОМ ЭФИРЕ", "kicker"),
            Ui.label("Не просто аккаунт.", "headline"),
            accent("Человек на занятии."),
            Ui.label(
                "QR связывает отметку с предметом. Подтверждение лица связывает её со студентом."
                    + " Bluetooth-сигнал преподавателя добавляет проверку близости к занятию.",
                "lede"),
            scene(second, 1),
            Ui.label(
                "Концепция проверки близости: телефон преподавателя — источник Bluetooth-сигнала,"
                    + " телефоны студентов — клиенты. Совместимость устройств, разрешения и защиту"
                    + " от подмены необходимо проверить в техническом пилоте.",
                "explanation"),
            Ui.label("✓ Три проверки — одна автоматическая отметка.", "lede"),
            grow(),
            new Separator(),
            Ui.label(
                "Одна пара. Один источник данных.                 Что происходит дальше ↓",
                "mono"));
    VBox third = chapter("third");
    third
        .getChildren()
        .addAll(
            grow(),
            Ui.label("● 03 / ОТ ДАННЫХ К ДЕЙСТВИЮ", "kicker"),
            Ui.label("Одна новая пара.", "headline"),
            accent("У всей группы."),
            Ui.label(
                "Назначьте занятие группе один раз. Оно появится в личном расписании каждого"
                    + " студента — без ручного заполнения десятков карточек.",
                "lede"),
            scene(third, 2),
            Ui.label(
                "Пустая пара — сигнал для проверки и подготовки повторного занятия. Академические"
                    + " задолженности и летний семестр согласуются по правилам университета.",
                "explanation"),
            grow(),
            new Separator(),
            Ui.label("TomorrowTOU / University operating system", "mono"),
            Ui.button("Войти в своё пространство ↗", () -> email.requestFocus()));
  }

  private Region grow() {
    Region r = new Region();
    VBox.setVgrow(r, Priority.ALWAYS);
    return r;
  }

  private Label accent(String s) {
    Label l = Ui.label(s, "headline");
    l.getStyleClass().add("accent");
    return l;
  }

  private Canvas scene(VBox parent, int index) {
    Canvas c = new Canvas(760, 385);
    c.widthProperty().bind(parent.widthProperty().subtract(96));
    c.widthProperty().addListener((o, a, b) -> draw(c, index));
    scenes.add(c);
    return c;
  }

  private void paint() {
    for (int i = 0; i < phases.size(); i++) {
      phases.get(i).getStyleClass().removeAll("active", "done");
      if (i == phase) phases.get(i).getStyleClass().add("active");
      else if (i < phase) phases.get(i).getStyleClass().add("done");
    }
    explanation.setText(COPY[phase]);
    for (int i = 0; i < scenes.size(); i++) draw(scenes.get(i), i);
  }

  private void draw(Canvas canvas, int index) {
    if (canvas.getWidth() <= 0) return;
    GraphicsContext g = canvas.getGraphicsContext2D();
    g.clearRect(0, 0, canvas.getWidth(), 385);
    g.save();
    g.scale(canvas.getWidth() / 760, 1);
    if (index == 0) {
      g.setStroke(Color.web("#c3dfa51b"));
      g.strokeOval(145, 95, 510, 190);
      g.strokeOval(225, 122, 330, 135);
      double orbit = motionSeconds * 2 * Math.PI / 30;
      g.setFill(Color.web("#f1ffe5"));
      g.fillOval(396 + 255 * Math.cos(orbit), 187 + 95 * Math.sin(orbit), 7, 7);
      for (int packet = 0; packet < 3; packet++) {
        double travel = ((motionSeconds + packet * 1.65) % 5) / 5;
        g.setGlobalAlpha(Math.sin(travel * Math.PI));
        g.setFill(Color.web("#d9ff85"));
        g.fillOval(140 + travel * 340, 200 - travel * 55, 8, 8);
      }
      g.setGlobalAlpha(1);
      g.setStroke(Color.web("#c5e5aa0a"));
      for (int i = 0; i < 16; i++) g.strokeLine(i * 50, 140, i * 50 - 100, 380);
      for (int i = 0; i < 7; i++) g.strokeLine(0, 160 + i * 28, 760, 160 + i * 28);
      g.save();
      g.translate(26, 79 + 1.5 - 6.5 * Math.cos(motionSeconds * Math.PI / 3));
      g.rotate(-3);
      box(g, 0, 0, 185, 246, "#26372a", "#60754a");
      text(g, "ПРЕПОДАВАТЕЛЬ / ПРЕДМЕТ", 16, 27, 9, "#b7c4ac");
      text(g, "Айгуль Сериковна", 16, 56, 14, "#f2f3eb");
      text(g, "Программирование · 402", 16, 78, 10, "#99ab94");
      g.setGlobalAlpha(phase < 2 ? .18 : 1);
      qr(g, 16, 101);
      g.setGlobalAlpha(1);
      text(
          g,
          phase == 0
              ? "→ Выбирает предмет"
              : phase == 1 ? "◎ Подтверждает лицо" : "✓ QR предмета активен",
          16,
          219,
          10,
          "#d9ff85");
      g.restore();
      g.save();
      g.translate(278, 35);
      g.rotate(1);
      box(g, 0, 0, 460, 315, "#121a17", "#65785d");
      text(g, "Табло присутствия", 20, 29, 12, "#f2f3eb");
      text(g, "● Live demo", 359, 29, 9, "#d9ff85");
      line(g, 20, 43, 440, 43);
      text(g, "CS-301 / Программирование", 20, 65, 10, "#718474");
      text(g, "09:00", 400, 65, 10, "#718474");
      int completed = student + (phase == 5 ? 1 : 0);
      for (int i = 0; i < 6; i++) {
        g.save();
        if (i == student && phase == 5 && !reducedMotion) {
          double t = Math.min(1, (motionSeconds - phaseStarted) / .65);
          double scale = Math.cos(Math.toRadians(75 * (1 - t)));
          double center = 92.5 + i * 30;
          g.translate(0, center);
          g.scale(1, scale);
          g.translate(0, -center);
        }
        g.setFill(Color.web(i < completed ? "#2d3c26" : "#202923"));
        g.fillRect(20, 79 + i * 30, 420, 27);
        text(g, PEOPLE[i], 29, 97 + i * 30, 11, "#f2f3eb");
        text(
            g,
            i < completed
                ? "✓ Присутствует"
                : i == student && phase == 3
                    ? "◎ Личность"
                    : i == student && phase == 4 ? "⌁ Рядом" : "· Ожидает",
            326,
            97 + i * 30,
            10,
            i < completed ? "#d9ff85" : "#6d7e6e");
        g.restore();
      }
      text(g, "Принято в журнал", 20, 287, 10, "#98aa8b");
      text(g, String.format("%02d", completed) + " / 06", 359, 287, 15, "#d9ff85");
      g.restore();
      box(g, 83, 352, 608, 30, "#23301e", "#667e4e");
      text(
          g,
          phase == 4
              ? "⌁ Проверка Bluetooth рядом с преподавателем"
              : phase == 5
                  ? "✓ " + PEOPLE[student] + " · отметка принята"
                  : PHASES[phase]
                      + " · "
                      + (phase >= 3 ? PEOPLE[student] : "Программирование / CS-301"),
          100,
          372,
          11,
          "#c9e3b1");
      g.setFill(Color.web("#263522"));
      g.fillOval(220, 240, 52, 52);
      text(g, phase == 4 ? "⌁" : phase == 5 ? "✓" : "◎", 236, 274, 25, "#d9ff85");
    } else if (index == 1) {
      box(g, 10, 35, 735, 300, "#131c19", "#5e7560");
      text(g, "Диспетчерская университета", 35, 68, 15, "#f2f3eb");
      text(g, "● Учебный процесс", 570, 68, 11, "#d9ff85");
      String[] subjects = {"Программирование", "Базы данных", "Алгоритмы"},
          times = {"09:00", "11:00", "14:00"},
          status = {"✓ В журнале", "● Приём отметок", "◷ По расписанию"};
      text(
          g,
          "ВРЕМЯ       ПРЕДМЕТ / НАПРАВЛЕНИЕ                     ГРУППА      СТАТУС",
          35,
          109,
          10,
          "#7a927b");
      for (int i = 0; i < 3; i++) {
        g.setFill(Color.web("#202c24"));
        g.fillRect(32, 128 + i * 49, 691, 43);
        text(g, times[i], 45, 155 + i * 49, 12, "#8da183");
        text(g, subjects[i], 116, 155 + i * 49, 13, "#f2f3eb");
        text(g, i == 2 ? "CS-301S" : "CS-301", 490, 155 + i * 49, 11, "#8ca380");
        text(g, status[i], 581, 155 + i * 49, 10, "#d9ff85");
      }
      text(
          g,
          "Преподаватель → QR → Студент                                     → Журнал ✓",
          35,
          312,
          12,
          "#90a584");
    } else {
      g.save();
      g.translate(60, 45);
      g.rotate(-4);
      box(g, 0, 0, 520, 249, "#232d20", "#819269");
      text(g, "CS-301 / РАСПИСАНИЕ ГРУППЫ", 25, 39, 12, "#b2c59c");
      String[] days = {"ПН", "ВТ", "СР", "ЧТ", "ПТ"};
      for (int i = 0; i < 5; i++) {
        box(g, 25 + i * 95, 70, 87, 125, "#283124", "#465039");
        text(g, days[i], 37 + i * 95, 95, 10, "#98aa85");
        if (i != 2) {
          double pulse = .5 - .5 * Math.cos(motionSeconds * Math.PI);
          g.setGlobalAlpha(reducedMotion ? 1 : .4 + .6 * pulse);
          g.setFill(Color.web("#70894f"));
          g.fillRoundRect(35 + i * 95, 114, 67, reducedMotion ? 40 : 32 + 8 * pulse, 5, 5);
          g.setGlobalAlpha(1);
        }
      }
      g.restore();
      box(g, 503, 237, 231, 111, "#d9ff85", "#d9ff85");
      text(g, "НАЗНАЧЕНИЕ СИНХРОНИЗИРОВАНО", 519, 263, 9, "#28361c");
      text(g, "+1 занятие", 519, 295, 25, "#28361c");
      text(g, "в личных расписаниях группы ✓", 519, 324, 11, "#28361c");
    }
    g.restore();
  }

  private void qr(GraphicsContext g, double x, double y) {
    g.setFill(Color.web("#e5efdc"));
    g.fillRect(x, y, 88, 88);
    g.setFill(Color.web("#263123"));
    for (int row = 0; row < 21; row++)
      for (int col = 0; col < 21; col++) {
        boolean on = false, found = false;
        for (int[] origin : new int[][] {{0, 0}, {14, 0}, {0, 14}}) {
          int dx = col - origin[0], dy = row - origin[1];
          if (dx >= 0 && dx < 7 && dy >= 0 && dy < 7) {
            on =
                dx == 0
                    || dx == 6
                    || dy == 0
                    || dy == 6
                    || dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4;
            found = true;
          }
        }
        if (!found) on = (col * 7 + row * 11 + col * row) % 5 < 2;
        if (on) g.fillRect(x + 5 + col * 3.7, y + 5 + row * 3.7, 3.7, 3.7);
      }
    g.setStroke(Color.web("#d9ff85"));
    g.strokeLine(
        x - 3,
        y + 5 + 80 * (.5 - .5 * Math.cos(motionSeconds * 2 * Math.PI / 3)),
        x + 91,
        y + 5 + 80 * (.5 - .5 * Math.cos(motionSeconds * 2 * Math.PI / 3)));
  }

  private void box(
      GraphicsContext g, double x, double y, double w, double h, String fill, String stroke) {
    g.setFill(Color.web(fill));
    g.fillRoundRect(x, y, w, h, 12, 12);
    g.setStroke(Color.web(stroke));
    g.strokeRoundRect(x, y, w, h, 12, 12);
  }

  private void line(GraphicsContext g, double x, double y, double xx, double yy) {
    g.setStroke(Color.web("#ffffff19"));
    g.strokeLine(x, y, xx, yy);
  }

  private void text(GraphicsContext g, String s, double x, double y, double size, String color) {
    g.setFont(Font.font("Manrope", size));
    g.setFill(Color.web(color));
    g.fillText(s, x, y);
  }

  @FXML
  private void about() {
    storyScroll.setVvalue(.5);
  }

  @FXML
  private void help() {
    Ui.notice(
        "Доступ к аккаунту",
        "Обратитесь к администратору университета. Администратор может назначить новый пароль в"
            + " разделе «Пользователи».");
  }

  @FXML
  private void togglePassword() {
    boolean show = !plainPassword.isVisible();
    plainPassword.setVisible(show);
    plainPassword.setManaged(show);
    password.setVisible(!show);
    password.setManaged(!show);
    showPassword.setText(show ? "Скрыть" : "Показать");
  }

  @FXML
  private void demo() {
    if (login.isDisable()) return;
    Toggle selected = roleGroup.getSelectedToggle();
    if (selected == null) {
      fail("Выберите роль");
      return;
    }
    role = selected.getUserData().toString();
    email.setText(
        switch (role) {
          case "ADMIN" -> "admin@tou.edu.kz";
          case "TEACHER" -> "teacher@tou.edu.kz";
          case "STAFF" -> "staff@tou.edu.kz";
          default -> "student@tou.edu.kz";
        });
    password.setText(System.getenv().getOrDefault("DEMO_PASSWORD", "Tomorrow21!"));
    login();
  }

  @FXML
  private void login() {
    if (login.isDisable()) return;
    if (email.getText().isBlank() || password.getText().isBlank()) {
      fail("Введите университетскую почту и пароль");
      return;
    }
    Toggle selected = roleGroup.getSelectedToggle();
    if (selected == null) {
      fail("Выберите роль");
      return;
    }
    role = selected.getUserData().toString();
    setLoginBusy(true);
    error.setVisible(false);
    error.setManaged(false);
    Map<String, String> body =
        Map.of("email", email.getText().trim(), "password", password.getText(), "role", role);
    boolean rememberAccount = remember.isSelected();
    Ui.async(
        () -> api.request("POST", "/auth/login", body),
        value -> {
          setLoginBusy(false);
          @SuppressWarnings("unchecked")
          Map<String, Object> result = (Map<String, Object>) value;
          api.token(result.get("token").toString());
          Preferences p = Preferences.userNodeForPackage(getClass());
          if (rememberAccount) {
            p.put("email", body.get("email"));
            p.put("role", body.get("role"));
          } else {
            p.remove("email");
            p.remove("role");
          }
          dispose();
          @SuppressWarnings("unchecked")
          Map<String, Object> user = (Map<String, Object>) result.get("user");
          entered.accept(user);
        },
        e -> {
          setLoginBusy(false);
          fail(
              e instanceof java.net.ConnectException
                  ? "API недоступен. Запустите backend: " + api.base()
                  : e.getMessage());
        });
  }

  private void setLoginBusy(boolean busy) {
    for (javafx.scene.Node control :
        List.of(login, demoButton, roles, email, password, plainPassword, showPassword, remember))
      control.setDisable(busy);
  }

  private void fail(String message) {
    error.setText(message == null ? "Не удалось войти" : message);
    error.setVisible(true);
    error.setManaged(true);
  }
}
