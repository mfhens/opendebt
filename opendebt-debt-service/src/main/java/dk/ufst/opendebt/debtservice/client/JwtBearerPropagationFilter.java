package dk.ufst.opendebt.debtservice.client;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

/**
 * Forwards the inbound resource-server JWT on outgoing WebClient calls so peer services authorize
 * the same end user (e.g. creditor-service validate-action with ROLE_CREDITOR).
 */
public final class JwtBearerPropagationFilter {

  private JwtBearerPropagationFilter() {}

  public static ExchangeFilterFunction create() {
    return (request, next) -> {
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication instanceof JwtAuthenticationToken jwt) {
        ClientRequest forwarded =
            ClientRequest.from(request)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getToken().getTokenValue())
                .build();
        return next.exchange(forwarded);
      }
      return next.exchange(request);
    };
  }
}
