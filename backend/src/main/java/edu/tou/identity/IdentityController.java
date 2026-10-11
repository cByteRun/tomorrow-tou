package edu.tou.identity;

import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class IdentityController {
  private final IdentityService identity;
  private volatile boolean ready;

  @org.springframework.context.event.EventListener(
      org.springframework.boot.context.event.ApplicationReadyEvent.class)
  public void ready() {
    ready = true;
  }

  public IdentityController(IdentityService i) {
    identity = i;
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", ready ? "UP" : "STARTING", "application", "tomorrow-tou");
  }

  @PostMapping("/auth/login")
  public Map<String, Object> login(@RequestBody Map<String, String> b) {
    if (!ready)
      throw new edu.tou.shared.BusinessException(
          503, "API ещё запускается. Дождитесь готовности сервера и повторите вход.");
    return identity.login(
        b.getOrDefault("email", ""), b.getOrDefault("password", ""), b.getOrDefault("role", ""));
  }

  @PostMapping("/auth/logout")
  public void logout(@RequestHeader("Authorization") String h) {
    identity.logout(h.substring(7));
  }

  @GetMapping("/profile")
  public Map<String, Object> profile() {
    return identity.view(identity.current());
  }

  @PutMapping("/profile")
  public Map<String, Object> profile(@RequestBody Map<String, Object> b) {
    return identity.profile(b);
  }
}
