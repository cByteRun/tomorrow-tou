package edu.tou.shared;

import static org.junit.jupiter.api.Assertions.*;

import edu.tou.workspace.Workspace;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

class FeatureContractsTest {
  @Test
  void everyDirectoryHasEditableFormAndVisibleColumns() {
    for (String key :
        List.of("students", "teachers", "groups", "subjects", "rooms", "semesters", "users")) {
      FeatureSchema schema = Workspace.schema(key);
      assertEquals(key, schema.key());
      assertFalse(schema.fields().isEmpty());
      assertFalse(schema.columns().isEmpty());
      assertEquals(
          schema.fields().size(), schema.fields().stream().map(FieldSpec::key).distinct().count());
    }
  }

  @Test
  void welcomeUsesNativeFXMLControls() throws Exception {
    var f = getClass().getResourceAsStream("/edu/tou/welcome/welcome.fxml");
    assertNotNull(f);
    var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    var document = factory.newDocumentBuilder().parse(f);
    assertEquals("SplitPane", document.getDocumentElement().getTagName());
    assertEquals(1, document.getElementsByTagName("PasswordField").getLength());
    assertEquals(0, document.getElementsByTagName("WebView").getLength());
  }

  @Test
  void roleLabelsIncludeSeparateAdminAndStaff() {
    assertEquals("Сотрудник", Ui.role("STAFF"));
    assertEquals("Администратор", Ui.role("ADMIN"));
    assertEquals("Студент", Ui.role("STUDENT"));
    assertEquals("Преподаватель", Ui.role("TEACHER"));
  }

  @Test
  void missingValueIsShownAsNoData() {
    assertEquals("—", Ui.str(null));
    assertEquals("0", Ui.str(0));
  }
}
