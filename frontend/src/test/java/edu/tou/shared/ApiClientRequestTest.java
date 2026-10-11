package edu.tou.shared;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiClientRequestTest {
  @Test
  void freshLoginDoesNotCarryPreviousSessionToken() throws Exception {
    ApiClient api = new ApiClient();
    api.token("expired-session-token");
    var request =
        api.buildRequest(
            "POST",
            "/auth/login",
            Map.of("email", "staff@tou.edu.kz", "password", "Tomorrow21!", "role", "STAFF"));
    assertTrue(request.headers().firstValue("Authorization").isEmpty());
    assertEquals("POST", request.method());
    assertEquals("/api/auth/login", request.uri().getPath());
  }

  @Test
  void cabinetRequestStillUsesCurrentSession() throws Exception {
    ApiClient api = new ApiClient();
    api.token("current-session-token");
    assertEquals(
        "Bearer current-session-token",
        api.buildRequest("GET", "/profile", null)
            .headers()
            .firstValue("Authorization")
            .orElseThrow());
  }

  @Test
  void logoutRemovesAuthorizationForFollowingRequests() throws Exception {
    ApiClient api = new ApiClient();
    api.token("previous-session-token");
    api.token("");
    assertTrue(
        api.buildRequest("GET", "/profile", null).headers().firstValue("Authorization").isEmpty());
  }
}
