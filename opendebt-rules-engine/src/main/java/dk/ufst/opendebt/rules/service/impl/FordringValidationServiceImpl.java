package dk.ufst.opendebt.rules.service.impl;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;
import dk.ufst.opendebt.rules.service.FordringValidationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Drools-based implementation of fordring validation.
 *
 * <p>Uses a separate KIE session with the "fordring-validation" agenda group to isolate fordring
 * rules from existing debt readiness, interest calculation, and collection priority rules.
 *
 * <p>Each invocation creates a new stateless session (no session persistence), inserts the request
 * and result objects, focuses the fordring-validation agenda group, fires all matching rules, and
 * then disposes the session.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FordringValidationServiceImpl implements FordringValidationService {

  static final String FORDRING_VALIDATION_AGENDA_GROUP = "fordring-validation";
  static final String RESULT_GLOBAL = "validationResult";

  private final KieContainer kieContainer;

  @Override
  public FordringValidationResult validateFordring(FordringValidationRequest request) {
    log.debug(
        "Validating fordring action: aktionKode={}, fordringhaveraftaleId={}",
        request.getAktionKode(),
        request.getFordringhaveraftaleId());

    FordringValidationResult result = FordringValidationResult.builder().build();

    KieSession kieSession = kieContainer.newKieSession();
    try {
      kieSession.setGlobal(RESULT_GLOBAL, result);
      kieSession.insert(request);
      kieSession.getAgenda().getAgendaGroup(FORDRING_VALIDATION_AGENDA_GROUP).setFocus();
      kieSession.fireAllRules();
    } finally {
      kieSession.dispose();
    }

    log.info(
        "Fordring validation for aktionKode={}: valid={}, errorCount={}",
        request.getAktionKode(),
        result.isValid(),
        result.getErrors().size());

    return result;
  }
}
