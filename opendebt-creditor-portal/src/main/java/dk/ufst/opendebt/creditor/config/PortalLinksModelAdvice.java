package dk.ufst.opendebt.creditor.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;

/**
 * Injects the portal external link URLs into every model so that templates can reference {@code
 * ${portalLinks.agreementMaterial}}, {@code ${portalLinks.contact}}, etc. without each controller
 * having to add them manually.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class PortalLinksModelAdvice {

  private final PortalLinksProperties portalLinksProperties;

  @ModelAttribute("portalLinks")
  public PortalLinksProperties portalLinks() {
    return portalLinksProperties;
  }
}
