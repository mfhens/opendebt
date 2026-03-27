package dk.ufst.opendebt.gateway.soap.filter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Serves WSDL files for GET requests to /soap/{name}?wsdl directly from classpath.
 *
 * <p>Spring-WS's MessageDispatcherServlet may reject GET requests or serve WSDLs with incorrect
 * content-type depending on handler adapter configuration. This filter intercepts qualifying GET
 * requests and streams the WSDL from the classpath with the correct {@code application/wsdl+xml}
 * MIME type, bypassing the servlet entirely.
 *
 * <p>Traceability: SPEC-019-02 (WSDL exposure)
 */
@Component
@Order(0)
public class WsdlServingFilter extends OncePerRequestFilter {

  private static final Set<String> ALLOWED_SERVICE_NAMES = Set.of("skat", "oio");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String query = request.getQueryString();
    if (!"GET".equalsIgnoreCase(request.getMethod())
        || query == null
        || !query.startsWith("wsdl")) {
      filterChain.doFilter(request, response);
      return;
    }

    String uri = request.getRequestURI();
    String serviceName = uri.substring(uri.lastIndexOf('/') + 1);

    if (!ALLOWED_SERVICE_NAMES.contains(serviceName)) {
      filterChain.doFilter(request, response);
      return;
    }

    String resourcePath = "wsdl/" + serviceName + "/" + serviceName + "-fordring.wsdl";

    // Use Spring ClassPathResource for reliable classpath loading in embedded-server tests
    ClassPathResource wsdlResource = new ClassPathResource(resourcePath);
    if (!wsdlResource.exists()) {
      filterChain.doFilter(request, response);
      return;
    }

    response.setContentType("application/wsdl+xml;charset=UTF-8");
    response.setStatus(HttpServletResponse.SC_OK);
    try (InputStream wsdlStream = wsdlResource.getInputStream()) {
      wsdlStream.transferTo(response.getOutputStream());
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Use getRequestURI() as fallback since getServletPath() may be empty before servlet dispatch
    String path = request.getServletPath();
    if (path == null || path.isEmpty()) {
      path = request.getRequestURI();
    }
    return path == null || !path.startsWith("/soap");
  }
}
