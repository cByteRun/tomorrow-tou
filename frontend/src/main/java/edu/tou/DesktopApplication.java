package edu.tou;

import edu.tou.shared.*;
import edu.tou.welcome.WelcomeController;
import edu.tou.workspace.Workspace;
import java.util.Map;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.stage.Stage;

public class DesktopApplication extends Application {
  private Stage stage;
  private final ApiClient api = new ApiClient();

  @Override
  public void start(Stage stage) throws Exception {
    BundledFonts.load();
    this.stage = stage;
    stage.setTitle("TomorrowTOU · Toraighyrov University");
    stage.setMinWidth(1100);
    stage.setMinHeight(740);
    welcome();
    stage.show();
  }

  public void welcome() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/tou/welcome/welcome.fxml"));
      Parent root = loader.load();
      WelcomeController controller = loader.getController();
      controller.configure(api, this::workspace);
      scene(root, "/edu/tou/welcome/welcome.css");
    } catch (Exception e) {
      Ui.error(e);
    }
  }

  private void workspace(Map<String, Object> user) {
    scene(new Workspace(api, user, this::welcome), "/edu/tou/workspace/app.css");
  }

  private void scene(Parent root, String css) {
    double w = stage.getScene() == null ? 1440 : stage.getScene().getWidth(),
        h = stage.getScene() == null ? 900 : stage.getScene().getHeight();
    Scene scene = new Scene(root, w, h);
    scene.getStylesheets().add(getClass().getResource(css).toExternalForm());
    stage.setScene(scene);
  }

  public static void main(String[] args) {
    launch(args);
  }
}
