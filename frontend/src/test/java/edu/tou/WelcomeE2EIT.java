package edu.tou;

import static org.junit.jupiter.api.Assertions.*;

import edu.tou.shared.ApiClient;
import edu.tou.welcome.WelcomeController;
import edu.tou.workspace.Workspace;
import java.util.Map;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Native JavaFX controls -> HTTP API -> PostgreSQL. Requires a running Docker stack and display.
 */
class WelcomeE2EIT {
  static <T> T fx(Callable<T> action) throws Exception {
    FutureTask<T> task = new FutureTask<>(action);
    Platform.runLater(task);
    return task.get(15, TimeUnit.SECONDS);
  }

  @BeforeAll
  static void toolkit() throws Exception {
    CountDownLatch ready = new CountDownLatch(1);
    Platform.startup(ready::countDown);
    assertTrue(ready.await(15, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    Platform.setImplicitExit(false);
    fx(
        () -> {
          edu.tou.shared.BundledFonts.load();
          return null;
        });
    assertEquals("UP", new ApiClient().object("/health").get("status"));
  }

  @AfterAll
  static void finish() {
    Platform.exit();
  }

  @ParameterizedTest
  @ValueSource(strings = {"STUDENT", "TEACHER", "STAFF", "ADMIN"})
  void selectRoleLoginAndFetchOwnProfile(String role) throws Exception {
    ApiClient api = new ApiClient();
    CompletableFuture<Map<String, Object>> entered = new CompletableFuture<>();
    Object[] window =
        fx(
            () -> {
              FXMLLoader loader =
                  new FXMLLoader(getClass().getResource("/edu/tou/welcome/welcome.fxml"));
              Parent root = loader.load();
              WelcomeController controller = loader.getController();
              controller.configure(api, entered::complete);
              Stage stage = new Stage();
              stage.setScene(new Scene(root, 1440, 900));
              stage
                  .getScene()
                  .getStylesheets()
                  .add(getClass().getResource("/edu/tou/welcome/welcome.css").toExternalForm());
              stage.show();
              root.applyCss();
              root.layout();
              // Exercise every intermediate role change, including reselecting the current toggle.
              for (String choice : new String[] {"ADMIN", "STUDENT", "STAFF", "TEACHER", role})
                for (var node : root.lookupAll(".toggle-button")) {
                  ToggleButton button = (ToggleButton) node;
                  if (choice.equals(button.getUserData())) button.fire();
                }
              ((TextField) root.lookup("#email")).setText(role.toLowerCase() + "@tou.edu.kz");
              ((PasswordField) root.lookup("#password")).setText("wrong-test-password");
              ((Button) root.lookup("#login")).fire();
              assertTrue(((Button) root.lookup("#demoButton")).isDisabled());
              assertTrue(root.lookup("#roles").isDisabled());
              ((Button) root.lookup("#demoButton")).fire();
              assertEquals(
                  "wrong-test-password", ((PasswordField) root.lookup("#password")).getText());
              return new Object[] {stage, controller, root};
            });
    try {
      long failureDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(40);
      while (fx(() -> ((Button) ((Parent) window[2]).lookup("#login")).isDisabled())
          && System.nanoTime() < failureDeadline) Thread.sleep(100);
      fx(
          () -> {
            Parent root = (Parent) window[2];
            assertFalse(((Button) root.lookup("#login")).isDisabled());
            assertEquals("Неверный логин или пароль", ((Label) root.lookup("#error")).getText());
            assertEquals(
                "wrong-test-password", ((PasswordField) root.lookup("#password")).getText());
            ((PasswordField) root.lookup("#password")).setText("Tomorrow21!");
            ((Button) root.lookup("#login")).fire();
            return null;
          });
      Map<String, Object> user;
      try {
        user = entered.get(40, TimeUnit.SECONDS);
      } catch (TimeoutException timeout) {
        String error = fx(() -> ((Label) ((Parent) window[2]).lookup("#error")).getText());
        throw new AssertionError("Login did not enter cabinet: " + error, timeout);
      }
      assertEquals(role, user.get("role"));
      assertEquals(user.get("email"), api.object("/profile").get("email"));
      CompletableFuture<Void> unexpectedExit = new CompletableFuture<>();
      Workspace workspace =
          fx(
              () -> {
                Workspace value = new Workspace(api, user, () -> unexpectedExit.complete(null));
                ((Stage) window[0]).getScene().setRoot(value);
                ((Stage) window[0])
                    .getScene()
                    .getStylesheets()
                    .setAll(getClass().getResource("/edu/tou/workspace/app.css").toExternalForm());
                return value;
              });
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(40);
      while (fx(() -> workspace.dashboard.isEmpty())
          && !unexpectedExit.isDone()
          && System.nanoTime() < deadline) Thread.sleep(100);
      assertFalse(
          unexpectedExit.isDone(), "A fresh login was incorrectly treated as an expired session");
      assertFalse(
          fx(() -> workspace.dashboard.isEmpty()), "Cabinet did not load the API dashboard");
      fx(
          () -> {
            workspace.navigate("schedule");
            workspace.navigate("profile");
            return null;
          });
      assertNotNull(api.object("/dashboard"));
      api.request("POST", "/auth/logout", null);
      assertEquals(
          401, assertThrows(ApiClient.ApiException.class, () -> api.object("/profile")).status);
    } finally {
      fx(
          () -> {
            ((WelcomeController) window[1]).dispose();
            ((Stage) window[0]).close();
            return null;
          });
    }
  }
}
