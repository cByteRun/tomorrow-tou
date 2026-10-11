package edu.tou.reports;

import edu.tou.audit.AuditService;
import edu.tou.grades.*;
import edu.tou.identity.IdentityService;
import edu.tou.identity.Role;
import edu.tou.shared.*;
import edu.tou.students.Student;
import edu.tou.university.UniversityService;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
  private final UniversityService service;
  private final IdentityService identity;
  private final AuditService audit;

  public ReportService(UniversityService s, IdentityService i, AuditService a) {
    service = s;
    identity = i;
    audit = a;
  }

  private List<Grade> grades(Long semester, Long group, Long student) {
    return service.visible("grades").stream()
        .map(x -> (Grade) x)
        .filter(
            g ->
                (semester == null || g.lesson.semester.id.equals(semester))
                    && (group == null || g.student.group.id.equals(group))
                    && (student == null || g.student.id.equals(student)))
        .toList();
  }

  @Transactional
  public byte[] xlsx(Long semester, Long group) throws IOException {
    if (identity.current().role == Role.STAFF)
      throw new BusinessException(403, "Отчёты успеваемости недоступны сотруднику");
    List<Grade> rows = grades(semester, group, null);
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Ведомость");
      String[] head = {
        "Студент", "Номер", "Группа", "Дисциплина", "Дата", "Тип", "Балл", "Семестр"
      };
      Row h = sheet.createRow(0);
      Font font = workbook.createFont();
      font.setBold(true);
      CellStyle style = workbook.createCellStyle();
      style.setFont(font);
      for (int i = 0; i < head.length; i++) {
        h.createCell(i).setCellValue(head[i]);
        h.getCell(i).setCellStyle(style);
      }
      int index = 1;
      for (Grade g : rows) {
        Row r = sheet.createRow(index++);
        String[] values = {
          g.student.firstName + " " + g.student.lastName,
          g.student.studentNo,
          g.student.group.name,
          g.lesson.subject.name,
          g.lesson.date.toString(),
          g.type,
          "",
          g.lesson.semester.name
        };
        for (int i = 0; i < values.length; i++) r.createCell(i).setCellValue(values[i]);
        r.getCell(6).setCellValue(g.value);
      }
      Row total = sheet.createRow(index);
      total.createCell(0).setCellValue("Средний балл");
      Double avg = GradeRules.average(rows);
      if (avg != null) total.createCell(6).setCellValue(avg);
      sheet.createFreezePane(0, 1);
      for (int i = 0; i < head.length; i++) sheet.setColumnWidth(i, 6000);
      workbook.write(bytes);
      audit.record(identity.current().email, "EXPORT", "xlsx", "Оценок: " + rows.size());
      return bytes.toByteArray();
    }
  }

  @Transactional
  public byte[] docx(long studentId, Long semester) throws IOException {
    Student s =
        service.visible("students").stream()
            .map(x -> (Student) x)
            .filter(x -> x.id.equals(studentId))
            .findFirst()
            .orElseThrow(() -> new BusinessException(403, "Студент недоступен"));
    List<Grade> rows = grades(semester, null, studentId);
    try (XWPFDocument document = new XWPFDocument();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
      paragraph(document, "АКАДЕМИЧЕСКАЯ СПРАВКА · Toraighyrov University", true);
      paragraph(document, "Студент: " + s.firstName + " " + s.lastName, false);
      paragraph(document, "Номер: " + s.studentNo + " · Группа: " + s.group.name, false);
      paragraph(
          document,
          "Дата: "
              + LocalDate.now()
              + " · Средний балл: "
              + Objects.toString(GradeRules.average(rows), "Нет оценок"),
          false);
      XWPFTable table = document.createTable(1, 3);
      String[] headers = {"Дисциплина", "Количество оценок", "Средний балл"};
      for (int i = 0; i < 3; i++) table.getRow(0).getCell(i).setText(headers[i]);
      Map<String, List<Grade>> grouped = new LinkedHashMap<>();
      for (Grade g : rows)
        grouped.computeIfAbsent(g.lesson.subject.name, k -> new ArrayList<>()).add(g);
      for (var entry : grouped.entrySet()) {
        XWPFTableRow r = table.createRow();
        r.getCell(0).setText(entry.getKey());
        r.getCell(1).setText("" + entry.getValue().size());
        r.getCell(2).setText("" + GradeRules.average(entry.getValue()));
      }
      document.write(bytes);
      audit.record(identity.current().email, "EXPORT", "docx", "Студент #" + studentId);
      return bytes.toByteArray();
    }
  }

  private void paragraph(XWPFDocument d, String text, boolean bold) {
    XWPFRun r = d.createParagraph().createRun();
    r.setBold(bold);
    r.setText(text);
    r.setFontSize(bold ? 16 : 11);
  }
}
