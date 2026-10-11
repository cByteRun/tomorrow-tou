package edu.tou.profile;

import edu.tou.shared.*;
import edu.tou.workspace.Workspace;
import java.util.Map;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class ProfileView extends VBox {
  public ProfileView(Workspace ws) {
    super(20);
    TextField name = new TextField(Ui.str(ws.user.get("name"))),
        email = new TextField(Ui.str(ws.user.get("email")));
    PasswordField password = new PasswordField();
    password.setPromptText("Оставьте пустым, чтобы сохранить пароль");
    getChildren()
        .addAll(
            Ui.label("Профиль и настройки", "page-title"),
            Ui.card(
                Ui.label(Ui.str(ws.user.get("name")), "section-title"),
                Ui.label(Ui.role(ws.role()), "subtle"),
                Ui.label("Имя", "field-label"),
                name,
                Ui.label("Университетская почта", "field-label"),
                email,
                Ui.label("Новый пароль (8–72 символа)", "field-label"),
                password,
                Ui.button(
                    "Сохранить профиль",
                    () -> {
                      if (name.getText().isBlank() || email.getText().isBlank()) {
                        Ui.notice("Профиль", "Заполните имя и почту");
                        return;
                      }
                      ws.perform(
                          () ->
                              ws.api.request(
                                  "PUT",
                                  "/profile",
                                  Map.of(
                                      "name",
                                      name.getText(),
                                      "email",
                                      email.getText(),
                                      "password",
                                      password.getText())),
                          result -> {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> updated = (Map<String, Object>) result;
                            ws.user = updated;
                            password.clear();
                            Ui.notice("Профиль сохранён", "Если пароль изменён, войдите повторно.");
                            ws.reload();
                          });
                    },
                    "btn-primary")),
            Ui.card(
                Ui.label("Настройки подключения", "section-title"),
                Ui.label(
                    "API: "
                        + ws.api.base()
                        + "\nАдрес задаётся через TOU_API_URL или -Dtou.api при запуске.",
                    "subtle")));
  }
}
