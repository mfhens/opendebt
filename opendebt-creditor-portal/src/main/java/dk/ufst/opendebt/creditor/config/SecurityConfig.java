package dk.ufst.opendebt.creditor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for creditor-portal in dev/demo mode. Permits all requests without
 * authentication.
 *
 * <p>This bean is ONLY active when the {@code dev} Spring profile is set (e.g. {@code
 * --spring.profiles.active=dev}). In all other environments OAuth2 client autoconfiguration (via
 * application.yml) enforces Keycloak login. Once a production-grade {@code .oauth2Login()} flow is
 * implemented, this class should be replaced by a profile-aware pair of SecurityFilterChain beans.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("dev")
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).csrf(csrf -> csrf.disable());
    return http.build();
  }
}
