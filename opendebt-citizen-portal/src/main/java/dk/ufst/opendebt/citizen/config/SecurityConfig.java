package dk.ufst.opendebt.citizen.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Autowired(required = false)
  private CitizenOidcUserService citizenOidcUserService;

  @Autowired(required = false)
  private ClientRegistrationRepository clientRegistrationRepository;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/",
                        "/was",
                        "/css/**",
                        "/js/**",
                        "/fonts/**",
                        "/webjars/**",
                        "/actuator/health",
                        "/error",
                        "/error/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .logout(
            logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID"));

    if (clientRegistrationRepository != null) {
      http.oauth2Login(
          oauth2 -> {
            oauth2
                .loginPage("/")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/error/login-failed");
            if (citizenOidcUserService != null) {
              oauth2.userInfoEndpoint(userInfo -> userInfo.oidcUserService(citizenOidcUserService));
            }
          });
    }

    return http.build();
  }
}
