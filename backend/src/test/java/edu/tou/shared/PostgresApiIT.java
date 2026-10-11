package edu.tou.shared;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"tou.demo-seed=true", "tou.demo-password=Tomorrow21!"})
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings("unchecked")
class PostgresApiIT {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired TestRestTemplate http;
  private final Map<String, String> tokens = new HashMap<>();

  @BeforeAll
  void login() {
    for (String role : List.of("ADMIN", "TEACHER", "STUDENT", "STAFF")) {
      String email = role.toLowerCase(Locale.ROOT) + "@tou.edu.kz";
      ResponseEntity<Map> response =
          http.postForEntity(
              "/api/auth/login",
              Map.of("email", email, "password", "Tomorrow21!", "role", role),
              Map.class);
      assertEquals(200, response.getStatusCode().value());
      tokens.put(role, response.getBody().get("token").toString());
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"ADMIN", "TEACHER", "STUDENT", "STAFF"})
  void freshSessionLoadsAllCabinetCollectionsAndDashboard(String role) {
    assertEquals(
        200, request(role, HttpMethod.GET, "/profile", null, Map.class).getStatusCode().value());
    for (String kind :
        List.of(
            "students",
            "teachers",
            "groups",
            "subjects",
            "rooms",
            "semesters",
            "users",
            "lessons",
            "attendance",
            "grades",
            "reviews",
            "audit"))
      assertEquals(
          200,
          request(role, HttpMethod.GET, "/records/" + kind, null, List.class)
              .getStatusCode()
              .value(),
          role + " / " + kind);
    assertEquals(
        200, request(role, HttpMethod.GET, "/dashboard", null, Map.class).getStatusCode().value());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"ADMIN", "TEACHER", "STUDENT", "STAFF"})
  void wrongPasswordDoesNotLockOutImmediateCorrectLogin(String role) {
    String email = role.toLowerCase(Locale.ROOT) + "@tou.edu.kz";
    for (int attempt = 0; attempt < 3; attempt++) {
      assertEquals(
          401,
          http.postForEntity(
                  "/api/auth/login",
                  Map.of("email", email, "password", "incorrect", "role", role),
                  Map.class)
              .getStatusCode()
              .value());
      var accepted =
          http.postForEntity(
              "/api/auth/login",
              Map.of("email", email, "password", "Tomorrow21!", "role", role),
              Map.class);
      assertEquals(200, accepted.getStatusCode().value());
      assertNotNull(accepted.getBody().get("token"));
    }
  }

  private <T> ResponseEntity<T> request(
      String role, HttpMethod method, String path, Object body, Class<T> type) {
    HttpHeaders h = new HttpHeaders();
    h.setBearerAuth(tokens.get(role));
    return http.exchange("/api" + path, method, new HttpEntity<>(body, h), type);
  }

  private List<Map<String, Object>> rows(String role, String kind) {
    return request(role, HttpMethod.GET, "/records/" + kind, null, List.class).getBody();
  }

  @Test
  void unauthenticatedRecordsAreRejected() {
    assertEquals(
        401, http.getForEntity("/api/records/grades", String.class).getStatusCode().value());
  }

  @Test
  void studentGetsOnlyOwnGrades() {
    var own = rows("STUDENT", "students");
    assertEquals(1, own.size());
    Object id = own.getFirst().get("id");
    assertTrue(rows("STUDENT", "grades").stream().allMatch(g -> g.get("studentId").equals(id)));
  }

  @Test
  void teacherGetsOnlyAssignedSubjects() {
    assertEquals(2, rows("TEACHER", "subjects").size());
    assertEquals(1, rows("TEACHER", "teachers").size());
  }

  @Test
  void staffCannotWriteGrades() {
    assertEquals(
        403,
        request(
                "STAFF",
                HttpMethod.POST,
                "/records/grades",
                Map.of("studentId", 1, "lessonId", 1, "type", "Текущая", "value", 80),
                Map.class)
            .getStatusCode()
            .value());
  }

  @Test
  void staffCanReviewTeacherLesson() {
    Map<String, Object> lesson =
        rows("STAFF", "lessons").stream()
            .filter(
                l ->
                    java.time.LocalDate.parse(l.get("date").toString())
                        .isBefore(java.time.LocalDate.now()))
            .findFirst()
            .orElseThrow();
    var created =
        request(
            "STAFF",
            HttpMethod.POST,
            "/records/reviews",
            Map.of(
                "lessonId",
                lesson.get("id"),
                "result",
                "confirmed",
                "note",
                "Проверено сотрудником"),
            Map.class);
    assertEquals(200, created.getStatusCode().value());
    Map row = created.getBody();
    assertEquals("confirmed", row.get("result"));
    assertEquals(
        200,
        request(
                "STAFF",
                HttpMethod.DELETE,
                "/records/reviews/" + row.get("id") + "?version=" + row.get("version"),
                null,
                String.class)
            .getStatusCode()
            .value());
  }

  @Test
  void scheduleRejectsConflictingInterval() {
    Map<String, Object> lesson = new LinkedHashMap<>(rows("ADMIN", "lessons").getFirst());
    lesson.remove("id");
    lesson.remove("version");
    assertEquals(
        409,
        request("ADMIN", HttpMethod.POST, "/records/lessons", lesson, Map.class)
            .getStatusCode()
            .value());
  }

  @Test
  void referentialIntegrityRejectsDeletingUsedStudent() {
    Map<String, Object> student = rows("ADMIN", "students").getFirst();
    assertEquals(
        409,
        request(
                "ADMIN",
                HttpMethod.DELETE,
                "/records/students/" + student.get("id") + "?version=" + student.get("version"),
                null,
                Map.class)
            .getStatusCode()
            .value());
  }

  @Test
  void dashboardReturnsDatabaseCounts() {
    Map data = request("ADMIN", HttpMethod.GET, "/dashboard", null, Map.class).getBody();
    assertEquals(rows("ADMIN", "students").size(), ((Number) data.get("students")).intValue());
    assertTrue(((Number) data.get("average")).doubleValue() > 0);
  }

  @Test
  void exportedXlsxOpensWithPoi() throws Exception {
    var response = request("STUDENT", HttpMethod.GET, "/reports/grades.xlsx", null, byte[].class);
    assertEquals(200, response.getStatusCode().value());
    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
      assertEquals(rows("STUDENT", "grades").size() + 1, workbook.getSheetAt(0).getLastRowNum());
    }
  }

  @Test
  void exportedDocxOpensWithPoi() throws Exception {
    Object id = rows("STUDENT", "students").getFirst().get("id");
    var response =
        request(
            "STUDENT", HttpMethod.GET, "/reports/student.docx?studentId=" + id, null, byte[].class);
    assertEquals(200, response.getStatusCode().value());
    try (var document = new XWPFDocument(new ByteArrayInputStream(response.getBody()))) {
      assertFalse(document.getTables().isEmpty());
    }
  }

  @Test
  void studentCannotExportSomeoneElsesAcademicRecord() {
    var me = rows("STUDENT", "students").getFirst().get("id");
    Object another =
        rows("ADMIN", "students").stream()
            .filter(s -> !s.get("id").equals(me))
            .findFirst()
            .orElseThrow()
            .get("id");
    assertEquals(
        403,
        request(
                "STUDENT",
                HttpMethod.GET,
                "/reports/student.docx?studentId=" + another,
                null,
                Map.class)
            .getStatusCode()
            .value());
  }

  @Test
  void expiredOrUnknownBearerIsRejected() {
    HttpHeaders h = new HttpHeaders();
    h.setBearerAuth("unknown");
    assertEquals(
        401,
        http.exchange("/api/profile", HttpMethod.GET, new HttpEntity<>(h), Map.class)
            .getStatusCode()
            .value());
  }
}
