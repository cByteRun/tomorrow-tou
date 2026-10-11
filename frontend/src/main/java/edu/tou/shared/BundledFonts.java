package edu.tou.shared;

import java.util.List;
import javafx.scene.text.Font;

public final class BundledFonts {
  private BundledFonts() {}

  public static final List<String> FILES =
      List.of(
          "Manrope-regular.ttf",
          "Manrope-medium.ttf",
          "Manrope-semibold.ttf",
          "Manrope-bold.ttf",
          "Manrope-extrabold.ttf",
          "Inter-Variable.ttf",
          "IBMPlexMono-Regular.ttf",
          "IBMPlexMono-Medium.ttf");

  public static void load() {
    for (String file : FILES) {
      try (var stream = BundledFonts.class.getResourceAsStream("/edu/tou/fonts/" + file)) {
        if (stream == null || Font.loadFont(stream, 12) == null)
          throw new IllegalStateException("Не удалось загрузить шрифт " + file);
      } catch (java.io.IOException e) {
        throw new IllegalStateException("Не удалось прочитать шрифт " + file, e);
      }
    }
  }
}
