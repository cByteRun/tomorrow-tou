package edu.tou.identity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.tou.audit.AuditService;
import edu.tou.shared.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

class IdentityServiceTest {
  private UserRepository users;
  private SessionRepository sessions;
  private PasswordEncoder passwords;
  private AuditService audit;
  private IdentityService service;
  private User u;

  @BeforeEach
  void setup() {
    users = mock(UserRepository.class);
    sessions = mock(SessionRepository.class);
    passwords = mock(PasswordEncoder.class);
    audit = mock(AuditService.class);
    service = new IdentityService(users, sessions, passwords, audit);
    u = Fixtures.user(Role.ADMIN, Fixtures.lesson(1));
    u.passwordHash = "hashed";
    when(users.findByEmailIgnoreCase(anyString()))
        .thenAnswer(
            i -> u.email.equalsIgnoreCase(i.getArgument(0)) ? Optional.of(u) : Optional.empty());
    when(passwords.matches("correct", u.passwordHash)).thenReturn(true);
  }

  @Test
  void validLoginReturnsOpaqueTokenAndSafeUser() {
    Map<String, Object> result = service.login(u.email, "correct", "ADMIN");
    assertEquals(43, result.get("token").toString().length());
    assertFalse(((Map<?, ?>) result.get("user")).containsKey("passwordHash"));
    verify(sessions).save(any());
  }

  @Test
  void selectedRoleMustMatchAccount() {
    assertThrows(BusinessException.class, () -> service.login(u.email, "correct", "STAFF"));
    verify(sessions, never()).save(any());
  }

  @Test
  void wrongPasswordDoesNotCreateSession() {
    assertThrows(BusinessException.class, () -> service.login(u.email, "wrong", "ADMIN"));
    verify(sessions, never()).save(any());
  }

  @Test
  void disabledAccountCannotLogin() {
    u.enabled = false;
    assertThrows(BusinessException.class, () -> service.login(u.email, "correct", "ADMIN"));
  }

  @Test
  void unknownEmailDoesNotRevealExistence() {
    BusinessException e =
        assertThrows(
            BusinessException.class, () -> service.login("unknown@tou.edu.kz", "correct", "ADMIN"));
    assertEquals(401, e.status);
    assertEquals("Неверный логин или пароль", e.getMessage());
  }

  @Test
  void emailLoginIsCaseInsensitive() {
    assertNotNull(service.login(u.email.toUpperCase(Locale.ROOT), "correct", "ADMIN").get("token"));
  }

  @Test
  void persistedTokenIsHashed() {
    var result = service.login(u.email, "correct", "ADMIN");
    ArgumentCaptor<AccessSession> cap = ArgumentCaptor.forClass(AccessSession.class);
    verify(sessions).save(cap.capture());
    assertNotEquals(result.get("token"), cap.getValue().tokenHash);
    assertEquals(64, cap.getValue().tokenHash.length());
    assertTrue(cap.getValue().expiresAt.isAfter(java.time.Instant.now()));
  }

  @Test
  void staffSpellingFromSpecificationIsAccepted() {
    u.role = Role.STAFF;
    assertNotNull(service.login(u.email, "correct", "STUFF").get("token"));
  }

  @Test
  void wrongRoleHasSpecificMessageAfterPasswordCheck() {
    BusinessException e =
        assertThrows(BusinessException.class, () -> service.login(u.email, "correct", "STUDENT"));
    assertEquals(403, e.status);
    assertTrue(e.getMessage().contains("роль"));
  }

  @Test
  void logoutRevokesSession() {
    AccessSession s = new AccessSession();
    s.user = u;
    when(sessions.findByTokenHash(IdentityService.hash("token"))).thenReturn(Optional.of(s));
    service.logout("token");
    verify(sessions).delete(s);
    verify(audit).record(u.email, "LOGOUT", "session", "Выход из системы");
  }
}
