package edu.tou.workspace;

import edu.tou.shared.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class EntityDialog {
  private EntityDialog() {}

  public record Choice(Object id, String label) {
    @Override
    public String toString() {
      return label;
    }
  }

  public static List<Choice> choices(Workspace ws, String source) {
    List<Choice> choices = new ArrayList<>();
    switch (source) {
      case "roles" -> {
        for (String r : List.of("ADMIN", "TEACHER", "STUDENT", "STAFF"))
          choices.add(new Choice(r, Ui.role(r)));
      }
      case "semesterStatuses" -> {
        choices.add(new Choice("active", "Активен"));
        choices.add(new Choice("archived", "Архив"));
      }
      case "lessonStatuses" -> {
        choices.add(new Choice("scheduled", "По расписанию"));
        choices.add(new Choice("conducted", "Проведено"));
        choices.add(new Choice("cancelled", "Отменено"));
      }
      case "gradeTypes" -> {
        for (String r : List.of("Текущая", "Рубежная", "Итоговая")) choices.add(new Choice(r, r));
      }
      case "attendanceStatuses" -> {
        choices.add(new Choice("present", "Присутствовал"));
        choices.add(new Choice("late", "Опоздал"));
        choices.add(new Choice("absent", "Отсутствовал"));
        choices.add(new Choice("excused", "Уважительная причина"));
      }
      case "reviewResults" -> {
        choices.add(new Choice("confirmed", "Проведение подтверждено"));
        choices.add(new Choice("issue", "Нарушение / замечание"));
        choices.add(new Choice("pending", "Требует проверки"));
      }
      default -> {
        for (Map<String, Object> r : ws.rows(source)) {
          String label =
              source.equals("lessons")
                  ? r.get("date")
                      + " · "
                      + r.get("start")
                      + " · "
                      + r.get("subjectName")
                      + " · "
                      + r.get("groupName")
                      + " · "
                      + r.get("teacherName")
                  : Ui.str(r.getOrDefault("name", r.getOrDefault("fullName", r.get("id"))));
          choices.add(new Choice(r.get("id"), label));
        }
      }
    }
    return choices;
  }

  public static ComboBox<Choice> select(Workspace ws, String source, Object value) {
    ComboBox<Choice> c = new ComboBox<>();
    c.getItems().setAll(choices(ws, source));
    c.setMaxWidth(Double.MAX_VALUE);
    c.setPromptText("Выберите…");
    for (Choice choice : c.getItems()) if (Objects.equals(choice.id(), value)) c.setValue(choice);
    return c;
  }

  public static void open(Workspace ws, FeatureSchema schema, Map<String, Object> original) {
    open(ws, schema, original, Map.of());
  }

  public static void open(
      Workspace ws,
      FeatureSchema schema,
      Map<String, Object> original,
      Map<String, Object> defaults) {
    if (!ws.canWrite(schema.key()) || original != null && !ws.canEdit(schema.key(), original))
      return;
    Map<String, Object> values = new LinkedHashMap<>(original == null ? defaults : original);
    Dialog<Void> dialog = new Dialog<>();
    dialog.setTitle((original == null ? "Добавление · " : "Изменение · ") + schema.title());
    dialog.initOwner(ws.getScene().getWindow());
    dialog
        .getDialogPane()
        .getStylesheets()
        .add(ws.getClass().getResource("/edu/tou/workspace/app.css").toExternalForm());
    ButtonType save = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, save);
    VBox form = new VBox(10);
    form.setPadding(new Insets(10));
    form.setPrefWidth(560);
    Map<String, Supplier<Object>> readers = new LinkedHashMap<>();
    Map<String, Node> controls = new LinkedHashMap<>();
    for (FieldSpec f : schema.fields()) {
      Object value = values.get(f.key());
      Node control;
      Supplier<Object> reader;
      switch (f.type()) {
        case "select" -> {
          ComboBox<Choice> combo =
              select(ws, f.source().equals("userEntities") ? "" : f.source(), value);
          control = combo;
          reader = () -> combo.getValue() == null ? null : combo.getValue().id();
        }
        case "multi" -> {
          VBox box = new VBox(6);
          List<CheckBox> checks = new ArrayList<>();
          for (Choice choice : choices(ws, f.source())) {
            CheckBox cb = new CheckBox(choice.label());
            cb.setUserData(choice.id());
            cb.setSelected(value instanceof Collection<?> c && c.contains(choice.id()));
            checks.add(cb);
            box.getChildren().add(cb);
          }
          control = box;
          reader =
              () -> checks.stream().filter(CheckBox::isSelected).map(Node::getUserData).toList();
        }
        case "date" -> {
          DatePicker picker =
              new DatePicker(value == null ? null : LocalDate.parse(value.toString()));
          picker.setMaxWidth(Double.MAX_VALUE);
          control = picker;
          reader = () -> picker.getValue() == null ? null : picker.getValue().toString();
        }
        case "boolean" -> {
          CheckBox cb = new CheckBox("Да");
          cb.setSelected(value == null || Boolean.TRUE.equals(value));
          control = cb;
          reader = cb::isSelected;
        }
        case "password" -> {
          PasswordField p = new PasswordField();
          p.setPromptText(
              original == null ? "Минимум 8 символов" : "Пустой — сохранить прежний пароль");
          control = p;
          reader = p::getText;
        }
        default -> {
          TextField text = new TextField(value == null ? "" : value.toString());
          text.setPromptText(f.label());
          control = text;
          reader =
              f.type().equals("number")
                  ? () -> Integer.valueOf(text.getText().trim())
                  : () -> text.getText().trim();
        }
      }
      controls.put(f.key(), control);
      readers.put(f.key(), reader);
      form.getChildren()
          .addAll(Ui.label(f.label() + (f.required() ? " *" : ""), "field-label"), control);
    }
    if (schema.key().equals("users")) {
      @SuppressWarnings("unchecked")
      ComboBox<Choice> role = (ComboBox<Choice>) controls.get("role");
      @SuppressWarnings("unchecked")
      ComboBox<Choice> entity = (ComboBox<Choice>) controls.get("entityId");
      Runnable change =
          () -> {
            String r = role.getValue() == null ? "" : role.getValue().id().toString();
            String source =
                r.equals("TEACHER") ? "teachers" : r.equals("STUDENT") ? "students" : "";
            entity.getItems().setAll(choices(ws, source));
            entity.setDisable(source.isEmpty());
            for (Choice c : entity.getItems())
              if (Objects.equals(c.id(), values.get("entityId"))) entity.setValue(c);
          };
      role.valueProperty().addListener((o, a, b) -> change.run());
      change.run();
    }
    Label error = Ui.label("", "inline-error");
    form.getChildren().add(error);
    ScrollPane scroll = new ScrollPane(form);
    scroll.setFitToWidth(true);
    scroll.setPrefViewportHeight(Math.min(600, 100 + schema.fields().size() * 78));
    dialog.getDialogPane().setContent(scroll);
    Button button = (Button) dialog.getDialogPane().lookupButton(save);
    button.disableProperty().bind(ws.disabledProperty());
    button.addEventFilter(
        javafx.event.ActionEvent.ACTION,
        event -> {
          event.consume();
          try {
            Map<String, Object> input = new LinkedHashMap<>(values);
            for (FieldSpec f : schema.fields()) {
              Object value = readers.get(f.key()).get();
              boolean required = f.required() && !(f.key().equals("password") && original != null);
              if (required
                  && (value == null
                      || value instanceof String s && s.isBlank()
                      || value instanceof Collection<?> c && c.isEmpty())) {
                error.setText("Заполните: " + f.label());
                controls.get(f.key()).requestFocus();
                return;
              }
              input.put(f.key(), value);
            }
            if ("cancelled".equals(input.get("status"))
                && schema.key().equals("lessons")
                && !Objects.equals(values.get("status"), "cancelled")
                && !Ui.confirm(
                    "Отменить занятие?",
                    "Студенты увидят занятие как отменённое. Новые оценки и отметки будут"
                        + " заблокированы.")) return;
            ws.save(schema.key(), original, input, dialog::close);
          } catch (Exception ex) {
            error.setText("Проверьте даты и целые числа в форме.");
          }
        });
    dialog.showAndWait();
  }
}
