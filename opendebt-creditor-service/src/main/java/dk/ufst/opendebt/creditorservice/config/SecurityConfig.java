package dk.ufst.opendebt.creditorservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for creditor-service. Configures OAuth2 resource server with JWT
 * authentication and permits unauthenticated access to actuator endpoints for Kubernetes
 * liveness/readiness probes.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /** Enable method-level @PreAuthorize in non-local/non-dev profiles only. */
  @Configuration
  @Profile("!local & !dev & !demo")
  @EnableMethodSecurity(prePostEnabled = true)
  static class ProductionMethodSecurity {}

  @Bean
  @Profile("!local & !dev & !demo")
  @SuppressWarnings(
      "java:S4502") // CSRF disabled intentionally - stateless JWT API, no session cookies
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/**")
                    .permitAll()
                    .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
    return http.build();
  }

  /** Permit all requests in the local profile for development and demo purposes. */
  @Bean
  @Profile("local | dev | demo")
  @SuppressWarnings("java:S4502")
  public SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }
}
