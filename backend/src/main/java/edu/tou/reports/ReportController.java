package edu.tou.reports;

import java.io.IOException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
  private final ReportService reports;

  public ReportController(ReportService r) {
    reports = r;
  }

  @GetMapping("/grades.xlsx")
  public ResponseEntity<byte[]> xlsx(
      @RequestParam(required = false) Long semesterId, @RequestParam(required = false) Long groupId)
      throws IOException {
    return download(
        reports.xlsx(semesterId, groupId),
        "grades.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  }

  @GetMapping("/student.docx")
  public ResponseEntity<byte[]> docx(
      @RequestParam long studentId, @RequestParam(required = false) Long semesterId)
      throws IOException {
    return download(
        reports.docx(studentId, semesterId),
        "student.docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
  }

  private ResponseEntity<byte[]> download(byte[] b, String name, String type) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
        .contentType(MediaType.parseMediaType(type))
        .body(b);
  }
}
