package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccessibilityControllerTest {

  private final AccessibilityController controller = new AccessibilityController();

  @Test
  void accessibilityStatement_returnsWasViewName() {
    String viewName = controller.accessibilityStatement();
    assertThat(viewName).isEqualTo("was");
  }
}
