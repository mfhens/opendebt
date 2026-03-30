package dk.ufst.opendebt.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables method-level {@code @PreAuthorize} / {@code @PostAuthorize} security. The filter chain
 * itself is provided by the keycloak-oauth2-starter auto-configuration.
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {}
