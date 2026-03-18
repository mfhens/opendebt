package dk.ufst.opendebt.caseworker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for caseworker-portal. Permits all requests for development/demo purposes
 * until Keycloak OAuth2 browser flow is fully wired. Once Keycloak is configured for browser login,
 * this should be updated to require authentication for portal pages and use {@code .oauth2Login()}
 * with the existing OAuth2 Client registration.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).csrf(csrf -> csrf.disable());
    return http.build();
  }
}
