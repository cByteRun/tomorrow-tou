package edu.tou.reports;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.tou.audit.AuditService;
import edu.tou.grades.Grade;
import edu.tou.identity.*;
import edu.tou.shared.*;
import edu.tou.students.Student;
import edu.tou.university.UniversityService;
import java.io.ByteArrayInputStream;
import java.util.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.*;

class ReportServiceTest {
  private UniversityService service;
  private IdentityService identity;
  private ReportService reports;
  private Grade grade;
  private Student student;

  @BeforeEach
  void setup() {
    service = mock(UniversityService.class);
    identity = mock(IdentityService.class);
    reports = new ReportService(service, identity, mock(AuditService.class));
    var l = Fixtures.lesson(1);
    student = Fixtures.student(1, l);
    student.studentNo = "TOU-3001";
    grade = new Grade();
    grade.student = student;
    grade.lesson = l;
    grade.value = 92;
    grade.type = "Текущая";
    when(identity.current()).thenReturn(Fixtures.user(Role.ADMIN, l));
    when(service.visible("grades")).thenReturn(List.of(grade));
    when(service.visible("students")).thenReturn(List.of(student));
  }

  @Test
  void xlsxContainsRealNamesAndNumericGrades() throws Exception {
    try (var book = new XSSFWorkbook(new ByteArrayInputStream(reports.xlsx(null, null)))) {
      var sheet = book.getSheetAt(0);
      assertEquals("First Last", sheet.getRow(1).getCell(0).getStringCellValue());
      assertEquals(92, sheet.getRow(1).getCell(6).getNumericCellValue());
      assertEquals(92, sheet.getRow(2).getCell(6).getNumericCellValue());
    }
  }

  @Test
  void xlsxRespectsSemesterFilter() throws Exception {
    try (var book = new XSSFWorkbook(new ByteArrayInputStream(reports.xlsx(999L, null)))) {
      assertEquals(1, book.getSheetAt(0).getLastRowNum());
      assertNull(book.getSheetAt(0).getRow(1).getCell(6));
    }
  }

  @Test
  void xlsxRespectsGroupFilter() throws Exception {
    try (var book = new XSSFWorkbook(new ByteArrayInputStream(reports.xlsx(null, 999L)))) {
      assertEquals(1, book.getSheetAt(0).getLastRowNum());
    }
  }

  @Test
  void docxContainsStudentAndActualAverage() throws Exception {
    try (var doc = new XWPFDocument(new ByteArrayInputStream(reports.docx(1, null)))) {
      assertTrue(doc.getParagraphs().stream().anyMatch(p -> p.getText().contains("First Last")));
      assertEquals("Программирование", doc.getTables().getFirst().getRow(1).getCell(0).getText());
      assertEquals("92.0", doc.getTables().getFirst().getRow(1).getCell(2).getText());
    }
  }

  @Test
  void docxCannotExportInaccessibleStudent() {
    assertEquals(403, assertThrows(BusinessException.class, () -> reports.docx(2, null)).status);
  }

  @Test
  void docxRespectsSemesterFilter() throws Exception {
    try (var doc = new XWPFDocument(new ByteArrayInputStream(reports.docx(1, 999L)))) {
      assertEquals(1, doc.getTables().getFirst().getNumberOfRows());
      assertTrue(doc.getParagraphs().stream().anyMatch(p -> p.getText().contains("Нет оценок")));
    }
  }
}
