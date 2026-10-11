package edu.tou.identity;

import edu.tou.audit.AuditService;
import edu.tou.shared.BusinessException;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {
  private final UserRepository users;
  private final SessionRepository sessions;
  private final PasswordEncoder passwords;
  private final AuditService audit;

  public IdentityService(UserRepository u, SessionRepository s, PasswordEncoder p, AuditService a) {
    users = u;
    sessions = s;
    passwords = p;
    audit = a;
  }

  @Transactional(noRollbackFor = BusinessException.class)
  public Map<String, Object> login(String email, String password, String role) {
    User u = users.findByEmailIgnoreCase(Objects.toString(email, "").trim()).orElse(null);
    if (u == null
        || !u.enabled
        || !passwords.matches(Objects.toString(password, ""), u.passwordHash)) {
      String reason =
          u == null ? "ACCOUNT_NOT_FOUND" : !u.enabled ? "ACCOUNT_DISABLED" : "PASSWORD_MISMATCH";
      audit.record(u == null ? "anonymous" : u.email, "LOGIN_FAILED", "session", reason);
      throw new BusinessException(401, "Неверный логин или пароль");
    }
    if (!matchesRole(u.role, role)) {
      audit.record(u.email, "LOGIN_FAILED", "session", "Выбранная роль не соответствует аккаунту");
      throw new BusinessException(
          403, "Выбранная роль не соответствует аккаунту. Выберите роль своего аккаунта.");
    }
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    AccessSession s = new AccessSession();
    s.tokenHash = hash(token);
    s.user = u;
    s.expiresAt = Instant.now().plus(Duration.ofHours(8));
    sessions.save(s);
    audit.record(u.email, "LOGIN", "session", "Вход в систему");
    return Map.of("token", token, "user", view(u));
  }

  private static boolean matchesRole(Role actual, String selected) {
    try {
      return actual == Role.parse(selected);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public User current() {
    String id = SecurityContextHolder.getContext().getAuthentication().getName();
    return users
        .findById(Long.valueOf(id))
        .filter(u -> u.enabled)
        .orElseThrow(() -> new BusinessException(401, "Сеанс недействителен"));
  }

  public Map<String, Object> view(User u) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", u.id);
    m.put("version", u.version);
    m.put("name", u.name);
    m.put("email", u.email);
    m.put("role", u.role.name());
    m.put("enabled", u.enabled);
    m.put("entityId", u.student != null ? u.student.id : u.teacher != null ? u.teacher.id : null);
    return m;
  }

  @Transactional
  public void logout(String token) {
    sessions
        .findByTokenHash(hash(token))
        .ifPresent(
            s -> {
              audit.record(s.user.email, "LOGOUT", "session", "Выход из системы");
              sessions.delete(s);
            });
  }

  @Transactional
  public Map<String, Object> profile(Map<String, Object> values) {
    User u = current();
    String name = Objects.toString(values.get("name"), "").trim(),
        email = Objects.toString(values.get("email"), "").trim().toLowerCase(Locale.ROOT);
    BusinessException.require(
        name.length() >= 2 && email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"),
        "Укажите имя и корректную почту");
    u.name = name;
    u.email = email;
    String p = Objects.toString(values.get("password"), "");
    if (!p.isEmpty()) {
      BusinessException.require(
          p.length() >= 8 && p.length() <= 72, "Пароль должен содержать 8–72 символа");
      u.passwordHash = passwords.encode(p);
      sessions.deleteByUserId(u.id);
    }
    users.saveAndFlush(u);
    audit.record(u.email, "UPDATE", "profile", "Профиль обновлён");
    return view(u);
  }
}
