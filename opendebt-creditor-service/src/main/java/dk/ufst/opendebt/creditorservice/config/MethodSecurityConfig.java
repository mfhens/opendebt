package dk.ufst.opendebt.creditorservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables method-level {@code @PreAuthorize} / {@code @PostAuthorize} security for non-local
 * profiles. The filter chain itself is provided by the keycloak-oauth2-starter auto-configuration.
 *
 * <p>Excluded from dev/local/test/demo profiles where the permissive filter chain is active.
 */
@Configuration
@Profile("!local & !dev & !demo & !test")
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {}
