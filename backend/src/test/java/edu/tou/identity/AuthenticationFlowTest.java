package edu.tou.identity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tou.audit.AuditService;
import edu.tou.shared.ApiErrors;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP integration through the real security chain; persistence is replaced by a session store. */
@WebMvcTest(IdentityController.class)
@Import({SecurityConfiguration.class, IdentityService.class, ApiErrors.class})
class AuthenticationFlowTest {
  @Autowired MockMvc http;
  @Autowired ObjectMapper json;
  @Autowired PasswordEncoder passwords;
  @MockitoBean UserRepository users;
  @MockitoBean SessionRepository sessions;
  @MockitoBean AuditService audit;
  final Map<String, AccessSession> store = new HashMap<>();
  User user;

  @BeforeEach
  void setup() {
    user = new User();
    user.id = 1L;
    user.name = "Тестовый пользователь";
    user.email = "admin@tou.edu.kz";
    user.role = Role.ADMIN;
    user.enabled = true;
    user.passwordHash = passwords.encode("Tomorrow21!");
    when(users.findByEmailIgnoreCase(anyString()))
        .thenAnswer(
            i ->
                user.email.equalsIgnoreCase(i.getArgument(0))
                    ? Optional.of(user)
                    : Optional.empty());
    when(users.findById(1L)).thenReturn(Optional.of(user));
    when(sessions.save(any()))
        .thenAnswer(
            i -> {
              AccessSession s = i.getArgument(0);
              store.put(s.tokenHash, s);
              return s;
            });
    when(sessions.findByTokenHash(anyString()))
        .thenAnswer(i -> Optional.ofNullable(store.get(i.getArgument(0))));
    doAnswer(
            i -> {
              AccessSession s = i.getArgument(0);
              store.remove(s.tokenHash);
              return null;
            })
        .when(sessions)
        .delete(any());
  }

  String login(Role role) throws Exception {
    user.role = role;
    user.email = role.name().toLowerCase(Locale.ROOT) + "@tou.edu.kz";
    String response =
        http.perform(
                post("/api/auth/login")
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "email",
                                user.email,
                                "password",
                                "Tomorrow21!",
                                "role",
                                role.name()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.role").value(role.name()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(response).get("token").asText();
  }

  @ParameterizedTest
  @EnumSource(Role.class)
  void loginFirstCabinetRequestAndLogoutForEveryRole(Role role) throws Exception {
    String token = login(role);
    http.perform(get("/api/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(user.email));
    http.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    http.perform(get("/api/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @EnumSource(Role.class)
  void failedLoginDoesNotTemporarilyRejectCorrectCredentials(Role role) throws Exception {
    String originalHash = user.passwordHash;
    for (int attempt = 0; attempt < 3; attempt++) {
      String firstToken = login(role);
      http.perform(
              post("/api/auth/login")
                  .contentType("application/json")
                  .content(
                      json.writeValueAsString(
                          Map.of(
                              "email", user.email, "password", "incorrect", "role", role.name()))))
          .andExpect(status().isUnauthorized());
      String nextToken = login(role);
      assertNotEquals(firstToken, nextToken);
      http.perform(get("/api/profile").header("Authorization", "Bearer " + nextToken))
          .andExpect(status().isOk());
      assertEquals(originalHash, user.passwordHash);
    }
  }

  @Test
  void expiredTokenCannotOpenCabinet() throws Exception {
    String token = login(Role.ADMIN);
    store.get(IdentityService.hash(token)).expiresAt = Instant.now().minusSeconds(1);
    http.perform(get("/api/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void disabledUserCannotReuseToken() throws Exception {
    String token = login(Role.STAFF);
    user.enabled = false;
    http.perform(get("/api/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void repositoryFailureAfterLoginIsServerErrorNotExpiredSession() throws Exception {
    String token = login(Role.ADMIN);
    when(users.findById(1L)).thenThrow(new IllegalStateException("database unavailable"));
    http.perform(get("/api/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void errorDispatchIsNotReclassifiedAsAuthenticationFailure() throws Exception {
    http.perform(
            get("/error")
                .with(
                    request -> {
                      request.setDispatcherType(jakarta.servlet.DispatcherType.ERROR);
                      return request;
                    }))
        .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()));
  }

  @Test
  void badPasswordIsCredentialErrorRatherThanRoleOrExpiryError() throws Exception {
    http.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of("email", user.email, "password", "wrong", "role", "ADMIN"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Неверный логин или пароль"));
    assertTrue(store.isEmpty());
  }

  @Test
  void healthIdentifiesThisApplicationWithoutLogin() throws Exception {
    http.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.application").value("tomorrow-tou"));
  }

  @Test
  void readinessIsOnlyReportedAfterApplicationRunnerCompletes() {
    IdentityController controller = new IdentityController(null);
    assertEquals("STARTING", controller.health().get("status"));
    var starting =
        assertThrows(
            edu.tou.shared.BusinessException.class,
            () ->
                controller.login(
                    Map.of(
                        "email", "staff@tou.edu.kz", "password", "Tomorrow21!", "role", "STAFF")));
    assertEquals(503, starting.status);
    controller.ready();
    assertEquals("UP", controller.health().get("status"));
  }

  @Test
  void missingTokenCannotOpenCabinet() throws Exception {
    http.perform(get("/api/profile")).andExpect(status().isUnauthorized());
  }

  @Test
  void wrongRoleDoesNotIssueToken() throws Exception {
    http.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of("email", user.email, "password", "Tomorrow21!", "role", "STUDENT"))))
        .andExpect(status().isForbidden());
    assertTrue(store.isEmpty());
  }
}
