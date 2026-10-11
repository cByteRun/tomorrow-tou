package edu.tou.shared;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Font;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BundledFontsTest {
  @ParameterizedTest
  @CsvSource({
    "Manrope-regular.ttf,Manrope",
    "Manrope-medium.ttf,Manrope Medium",
    "Manrope-semibold.ttf,Manrope SemiBold",
    "Manrope-bold.ttf,Manrope",
    "Manrope-extrabold.ttf,Manrope ExtraBold",
    "Inter-Variable.ttf,Inter",
    "IBMPlexMono-Regular.ttf,IBM Plex Mono",
    "IBMPlexMono-Medium.ttf,IBM Plex Mono Medium"
  })
  void originalFontFilesLoadAndContainRussianAlphabet(String file, String family) throws Exception {
    try (var stream = getClass().getResourceAsStream("/edu/tou/fonts/" + file)) {
      assertNotNull(stream, "Bundled font missing: " + file);
      Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
      assertEquals(family, font.getFamily(java.util.Locale.ROOT));
      assertEquals(
          -1,
          font.canDisplayUpTo(
              "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя"));
    }
  }
}
