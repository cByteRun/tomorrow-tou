package edu.tou.identity;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfiguration {
  @Bean
  PasswordEncoder passwords() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, SessionRepository sessions) throws Exception {
    OncePerRequestFilter bearer =
        new OncePerRequestFilter() {
          protected void doFilterInternal(
              HttpServletRequest req, HttpServletResponse res, FilterChain chain)
              throws IOException, ServletException {
            String h = req.getHeader("Authorization");
            if (h != null && h.startsWith("Bearer "))
              sessions
                  .findByTokenHash(IdentityService.hash(h.substring(7)))
                  .filter(s -> s.expiresAt.isAfter(Instant.now()) && s.user.enabled)
                  .ifPresent(
                      s ->
                          SecurityContextHolder.getContext()
                              .setAuthentication(
                                  new UsernamePasswordAuthenticationToken(
                                      s.user.id.toString(),
                                      null,
                                      List.of(new SimpleGrantedAuthority("ROLE_" + s.user.role)))));
            chain.doFilter(req, res);
          }
        };
    return http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
                    .permitAll()
                    .requestMatchers("/api/auth/login", "/api/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                    (q, r, x) -> {
                      r.setStatus(401);
                      r.setContentType("application/json;charset=UTF-8");
                      r.getWriter().write("{\"message\":\"Войдите в систему\"}");
                    }))
        .addFilterBefore(bearer, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
