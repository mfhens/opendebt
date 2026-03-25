package dk.ufst.opendebt.gateway.soap.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that preserves custom SOAP fault HTTP status codes set by {@link
 * dk.ufst.opendebt.gateway.soap.fault.SoapFaultMappingResolver}.
 *
 * <p>Spring-WS unconditionally sets HTTP 500 for all SOAP fault responses in {@code
 * HttpServletConnection.send()}. When our fault resolver writes a custom status (401, 403, 400)
 * into request attribute {@code soap.desired.http.status}, this filter intercepts subsequent {@code
 * setStatus()} calls and substitutes the desired status, ensuring OCES3 security and validation
 * faults are returned with semantically correct HTTP codes.
 *
 * <p>Traceability: SPEC-019-06 (SoapFaultMappingResolver), AC-07, AC-08, AC-09, AC-14
 */
@Component
@Order(1)
public class SoapHttpStatusFilter extends OncePerRequestFilter {

  /** Request attribute key written by {@code SoapFaultMappingResolver}. */
  public static final String DESIRED_STATUS_ATTR = "soap.desired.http.status";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    HttpServletResponseWrapper wrapper =
        new HttpServletResponseWrapper(response) {
          private int resolve(int sc) {
            Integer desired = (Integer) request.getAttribute(DESIRED_STATUS_ATTR);
            return desired != null ? desired : sc;
          }

          @Override
          public void setStatus(int sc) {
            super.setStatus(resolve(sc));
          }

          @Override
          public void sendError(int sc) throws IOException {
            super.setStatus(resolve(sc));
          }

          @Override
          public void sendError(int sc, String msg) throws IOException {
            super.setStatus(resolve(sc));
          }
        };

    filterChain.doFilter(request, wrapper);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Only apply to SOAP endpoints
    String path = request.getServletPath();
    return path == null || !path.startsWith("/soap");
  }
}
