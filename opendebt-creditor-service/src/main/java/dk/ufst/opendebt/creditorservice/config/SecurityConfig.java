package dk.ufst.opendebt.creditorservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  @Bean
  @SuppressWarnings(
      "java:S4502") // CSRF disabled intentionally - stateless JWT API, no session cookies
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    // CSRF protection is disabled because this is a stateless REST API using JWT bearer tokens.
    // CSRF attacks exploit session cookies, which are not used here. All authentication is via
    // Authorization header with OAuth2/JWT tokens. See OWASP CSRF Prevention Cheat Sheet.
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
}
