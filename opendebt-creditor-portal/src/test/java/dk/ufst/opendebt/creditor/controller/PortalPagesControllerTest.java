package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

@ExtendWith(MockitoExtension.class)
class PortalPagesControllerTest {

  @InjectMocks private PortalPagesController controller;

  @Test
  void sager_returnsSagerViewName() {
    Model model = new ConcurrentModel();
    assertThat(controller.sager(model)).isEqualTo("sager");
    assertThat(model.getAttribute("currentPage")).isEqualTo("cases");
  }
}
