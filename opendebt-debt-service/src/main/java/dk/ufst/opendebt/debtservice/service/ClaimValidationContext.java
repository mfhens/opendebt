package dk.ufst.opendebt.debtservice.service;

/**
 * Carries the ingress context required to map claim submissions onto the shared fordring Drools
 * validation contract.
 */
public record ClaimValidationContext(
    ClaimIngressPath ingressPath,
    boolean claimCreationAuthorized,
    boolean systemReporterAuthorized,
    boolean portalAgreement,
    boolean validSsoAccess) {

  public ClaimValidationContext {
    ingressPath = ingressPath != null ? ingressPath : ClaimIngressPath.INTERNAL;
  }

  public static ClaimValidationContext systemToSystem() {
    return new ClaimValidationContext(ClaimIngressPath.SYSTEM_TO_SYSTEM, true, true, false, false);
  }

  public static ClaimValidationContext portal() {
    return new ClaimValidationContext(ClaimIngressPath.PORTAL, true, false, true, true);
  }

  public static ClaimValidationContext internal() {
    return new ClaimValidationContext(ClaimIngressPath.INTERNAL, true, false, false, false);
  }

  public static ClaimValidationContext fromSubmitHeader(String headerValue) {
    ClaimIngressPath path =
        ClaimIngressPath.fromHeader(headerValue, ClaimIngressPath.SYSTEM_TO_SYSTEM);
    return path == ClaimIngressPath.PORTAL ? portal() : systemToSystem();
  }

  public boolean isPortalSubmission() {
    return ingressPath == ClaimIngressPath.PORTAL;
  }

  public boolean isSystemToSystemSubmission() {
    return ingressPath == ClaimIngressPath.SYSTEM_TO_SYSTEM;
  }

  public enum ClaimIngressPath {
    SYSTEM_TO_SYSTEM,
    PORTAL,
    INTERNAL;

    static ClaimIngressPath fromHeader(String headerValue, ClaimIngressPath defaultPath) {
      if (headerValue == null || headerValue.isBlank()) {
        return defaultPath;
      }
      try {
        return ClaimIngressPath.valueOf(headerValue.trim().toUpperCase());
      } catch (IllegalArgumentException ex) {
        return defaultPath;
      }
    }
  }
}
