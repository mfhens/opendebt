package dk.ufst.opendebt.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables method-level {@code @PreAuthorize} / {@code @PostAuthorize} security. The filter chain
 * itself is provided by the keycloak-oauth2-starter auto-configuration.
 *
 * <p>Excluded from dev/local profiles where the permissive filter chain is active and
 * server-to-server calls from portals carry no auth token. Active in the test profile so that
 * role-based access control is verified by Cucumber security scenarios.
 */
@Configuration
@EnableMethodSecurity
@Profile("!dev & !local")
public class MethodSecurityConfig {}
