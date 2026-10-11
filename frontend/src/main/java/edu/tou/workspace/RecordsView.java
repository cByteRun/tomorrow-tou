package edu.tou.workspace;

import edu.tou.shared.*;
import java.util.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.*;
import javafx.collections.transformation.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class RecordsView extends VBox {
  private final Workspace ws;
  private final FeatureSchema schema;
  private final TableView<Map<String, Object>> table = new TableView<>();
  private final FilteredList<Map<String, Object>> filtered;
  private final SortedList<Map<String, Object>> sorted;
  private final Label count = Ui.label("", "subtle"), pages = Ui.label("", "subtle");
  private int page;
  private final int size = 10;
  private final TextField search = new TextField();
  private final ComboBox<EntityDialog.Choice> filter = new ComboBox<>();

  public RecordsView(Workspace ws, FeatureSchema schema) {
    this(ws, schema, ws.rows(schema.key()));
  }

  public RecordsView(Workspace ws, FeatureSchema schema, List<Map<String, Object>> rows) {
    super(20);
    this.ws = ws;
    this.schema = schema;
    filtered = new FilteredList<>(FXCollections.observableArrayList(rows), x -> true);
    sorted = new SortedList<>(filtered);
    sorted.comparatorProperty().bind(table.comparatorProperty());
    getChildren()
        .addAll(Ui.label(schema.title(), "page-title"), Ui.label(schema.description(), "subtle"));
    search.setPromptText("Поиск по имени, группе, дисциплине…");
    search.setPrefWidth(320);
    HBox toolbar = new HBox(10, search);
    toolbar.setAlignment(Pos.CENTER_LEFT);
    if (!schema.filterKey().isEmpty()) {
      String source =
          switch (schema.filterKey()) {
            case "groupId", "id" -> "groups";
            case "subjectIds" -> "subjects";
            case "lessonId" -> "lessons";
            case "role" -> "roles";
            default -> "";
          };
      filter.getItems().add(new EntityDialog.Choice(null, "Все"));
      filter.getItems().addAll(EntityDialog.choices(ws, source));
      filter.getSelectionModel().selectFirst();
      filter.setPrefWidth(250);
      toolbar.getChildren().add(filter);
      filter.valueProperty().addListener((o, a, b) -> apply());
    }
    Region space = new Region();
    HBox.setHgrow(space, Priority.ALWAYS);
    toolbar.getChildren().addAll(space, count);
    if (ws.canWrite(schema.key()))
      toolbar
          .getChildren()
          .add(Ui.button("+ Добавить", () -> EntityDialog.open(ws, schema, null), "btn-primary"));
    search.textProperty().addListener((o, a, b) -> apply());
    for (FeatureSchema.Column c : schema.columns()) {
      TableColumn<Map<String, Object>, String> column = new TableColumn<>(c.label());
      column.setCellValueFactory(
          v -> new ReadOnlyStringWrapper(display(c.key(), v.getValue().get(c.key()))));
      column.setComparator(
          (a, b) -> {
            try {
              return Double.compare(Double.parseDouble(a), Double.parseDouble(b));
            } catch (Exception e) {
              return java.text.Collator.getInstance(Locale.forLanguageTag("ru")).compare(a, b);
            }
          });
      column.setPrefWidth(c.key().contains("Name") || c.key().equals("name") ? 200 : 140);
      table.getColumns().add(column);
    }
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.setPlaceholder(Ui.label("Записей нет. Измените фильтр или добавьте запись.", "subtle"));
    table.setPrefHeight(480);
    table.setFixedCellSize(45);
    table
        .comparatorProperty()
        .addListener(
            (o, a, b) -> {
              page = 0;
              update();
            });
    Button detail = Ui.button("Карточка", () -> selected(this::detail), "btn-outline");
    Button edit =
        Ui.button("Изменить", () -> selected(r -> EntityDialog.open(ws, schema, r)), "btn-soft");
    Button delete =
        Ui.button("Удалить", () -> selected(r -> ws.delete(schema.key(), r)), "btn-danger");
    detail.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
    edit.disableProperty()
        .bind(
            javafx.beans.binding.Bindings.createBooleanBinding(
                () ->
                    table.getSelectionModel().getSelectedItem() == null
                        || !ws.canEdit(schema.key(), table.getSelectionModel().getSelectedItem()),
                table.getSelectionModel().selectedItemProperty()));
    delete
        .disableProperty()
        .bind(
            javafx.beans.binding.Bindings.createBooleanBinding(
                () ->
                    table.getSelectionModel().getSelectedItem() == null
                        || !ws.canDelete(schema.key(), table.getSelectionModel().getSelectedItem()),
                table.getSelectionModel().selectedItemProperty()));
    HBox actions = new HBox(8, detail);
    if (ws.canWrite(schema.key())) actions.getChildren().addAll(edit, delete);
    table.setRowFactory(
        t -> {
          TableRow<Map<String, Object>> row = new TableRow<>();
          row.setOnMouseClicked(
              e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) detail(row.getItem());
              });
          return row;
        });
    HBox footer =
        new HBox(
            10,
            Ui.button(
                "←",
                () -> {
                  if (page > 0) {
                    page--;
                    update();
                  }
                },
                "btn-outline"),
            pages,
            Ui.button(
                "→",
                () -> {
                  if ((page + 1) * size < sorted.size()) {
                    page++;
                    update();
                  }
                },
                "btn-outline"),
            new Region(),
            actions);
    footer.setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(footer.getChildren().get(3), Priority.ALWAYS);
    getChildren().add(Ui.card(toolbar, table, footer));
    update();
  }

  private String display(String key, Object value) {
    if (key.equals("role") && value != null) return Ui.role(value.toString());
    if (key.equals("status") || key.equals("result"))
      return switch (Objects.toString(value, "")) {
        case "active" -> "Активен";
        case "archived" -> "Архив";
        case "scheduled" -> "По расписанию";
        case "conducted" -> "Проведено";
        case "cancelled" -> "Отменено";
        case "present" -> "Присутствовал";
        case "late" -> "Опоздал";
        case "absent" -> "Отсутствовал";
        case "excused" -> "Уважительная";
        case "confirmed" -> "Подтверждено";
        case "issue" -> "Замечание";
        case "pending" -> "Требует проверки";
        default -> Ui.str(value);
      };
    return value instanceof Boolean b ? b ? "Да" : "Нет" : Ui.str(value);
  }

  private void selected(java.util.function.Consumer<Map<String, Object>> action) {
    Map<String, Object> row = table.getSelectionModel().getSelectedItem();
    if (row != null) action.accept(row);
  }

  private void apply() {
    String q = search.getText().trim().toLowerCase(Locale.ROOT);
    Object id = filter.getValue() == null ? null : filter.getValue().id();
    filtered.setPredicate(
        row ->
            row.values().stream().map(Ui::str).anyMatch(s -> s.toLowerCase(Locale.ROOT).contains(q))
                && (id == null
                    || row.get(schema.filterKey()) instanceof Collection<?> c && c.contains(id)
                    || Objects.equals(id, row.get(schema.filterKey()))));
    page = 0;
    update();
  }

  private void update() {
    int max = Math.max(1, (int) Math.ceil(sorted.size() / (double) size));
    page = Math.min(page, max - 1);
    int begin = Math.min(page * size, sorted.size()), end = Math.min(begin + size, sorted.size());
    table.setItems(FXCollections.observableArrayList(sorted.subList(begin, end)));
    count.setText("Записей: " + sorted.size());
    pages.setText((page + 1) + " / " + max);
  }

  private void detail(Map<String, Object> row) {
    Dialog<Void> d = new Dialog<>();
    d.initOwner(getScene().getWindow());
    d.setTitle("Карточка · " + schema.title());
    d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    VBox body = new VBox(10);
    for (FeatureSchema.Column c : schema.columns())
      body.getChildren()
          .add(Ui.label(c.label() + ": " + display(c.key(), row.get(c.key())), "subtle"));
    for (FieldSpec f : schema.fields())
      if (!f.key().equals("password")
          && schema.columns().stream().noneMatch(c -> c.key().equals(f.key()))) {
        Object value = row.get(f.key());
        String shown =
            f.source().isBlank()
                ? Ui.str(value)
                : f.type().equals("multi") && value instanceof Collection<?> ids
                    ? String.join(", ", ids.stream().map(id -> ws.name(f.source(), id)).toList())
                    : ws.name(f.source(), value);
        body.getChildren().add(Ui.label(f.label() + ": " + shown, "subtle"));
      }
    if (schema.key().equals("teachers")) {
      body.getChildren().add(Ui.label("Расписание преподавателя", "section-title"));
      for (Map<String, Object> l : ws.rows("lessons"))
        if (Objects.equals(l.get("teacherId"), row.get("id")))
          body.getChildren()
              .add(
                  Ui.label(
                      l.get("date")
                          + " "
                          + l.get("start")
                          + " · "
                          + l.get("subjectName")
                          + " · "
                          + l.get("groupName"),
                      "subtle"));
    }
    if (schema.key().equals("groups")) {
      body.getChildren().add(Ui.label("Состав группы", "section-title"));
      for (Map<String, Object> s : ws.rows("students"))
        if (Objects.equals(s.get("groupId"), row.get("id")))
          body.getChildren()
              .add(Ui.label(Ui.str(s.get("fullName")) + " · " + s.get("studentNo"), "subtle"));
    }
    ScrollPane scroll = new ScrollPane(body);
    scroll.setFitToWidth(true);
    scroll.setPrefViewportWidth(620);
    scroll.setPrefViewportHeight(420);
    d.getDialogPane().setContent(scroll);
    d.getDialogPane()
        .getStylesheets()
        .add(getClass().getResource("/edu/tou/workspace/app.css").toExternalForm());
    d.showAndWait();
  }
}
