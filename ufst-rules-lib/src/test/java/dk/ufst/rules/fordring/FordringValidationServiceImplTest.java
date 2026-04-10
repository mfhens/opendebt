package dk.ufst.rules.fordring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.Agenda;
import org.kie.api.runtime.rule.AgendaGroup;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;
import dk.ufst.rules.service.impl.FordringValidationServiceImpl;

@ExtendWith(MockitoExtension.class)
class FordringValidationServiceImplTest {

  private static final String FORDRING_VALIDATION_AGENDA_GROUP = "fordring-validation";
  private static final String RESULT_GLOBAL = "validationResult";

  @Mock private KieContainer kieContainer;
  @Mock private KieSession kieSession;
  @Mock private Agenda agenda;
  @Mock private AgendaGroup agendaGroup;

  @InjectMocks private FordringValidationServiceImpl service;

  @BeforeEach
  void setUp() {
    when(kieContainer.newKieSession()).thenReturn(kieSession);
    when(kieSession.getAgenda()).thenReturn(agenda);
    when(agenda.getAgendaGroup(FORDRING_VALIDATION_AGENDA_GROUP)).thenReturn(agendaGroup);
  }

  @Test
  void validateFordring_shouldCreateSessionAndFireRules() {
    FordringValidationRequest request =
        FordringValidationRequest.builder().aktionKode("OPRETFORDRING").build();

    FordringValidationResult result = service.validateFordring(request);

    assertThat(result).isNotNull();
    assertThat(result.isValid()).isTrue();

    verify(kieContainer).newKieSession();
    verify(kieSession).setGlobal(eq(RESULT_GLOBAL), any());
    verify(kieSession).insert(request);
    verify(agendaGroup).setFocus();
    verify(kieSession).fireAllRules();
    verify(kieSession).dispose();
  }

  @Test
  void validateFordring_shouldDisposeSessionOnException() {
    when(kieSession.fireAllRules()).thenThrow(new RuntimeException("Rule engine error"));

    FordringValidationRequest request =
        FordringValidationRequest.builder().aktionKode("OPRETFORDRING").build();

    try {
      service.validateFordring(request);
    } catch (RuntimeException e) {
      // Expected
    }

    verify(kieSession).dispose();
  }

  @Test
  void validateFordring_shouldUseFordringValidationAgendaGroup() {
    FordringValidationRequest request =
        FordringValidationRequest.builder().aktionKode("NEDSKRIV").build();

    service.validateFordring(request);

    verify(agenda).getAgendaGroup(FORDRING_VALIDATION_AGENDA_GROUP);
    verify(agendaGroup).setFocus();
  }
}
