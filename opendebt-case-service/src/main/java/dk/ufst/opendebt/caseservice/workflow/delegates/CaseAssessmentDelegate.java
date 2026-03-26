package dk.ufst.opendebt.caseservice.workflow.delegates;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Delegate for automatic case assessment at workflow start. Determines initial collection strategy
 * based on case characteristics.
 */
@Slf4j
@Component("caseAssessmentDelegate")
@RequiredArgsConstructor
public class CaseAssessmentDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) {
    String caseId = (String) execution.getVariable("caseId");
    log.info("Assessing case: {}", caseId);

    // AIDEV-TODO: Call rules engine to assess case and determine strategy
    // For now, use the strategy passed in or default to VOLUNTARY_PAYMENT

    String strategy = (String) execution.getVariable("collectionStrategy");
    if (strategy == null || strategy.isEmpty()) {
      strategy = "VOLUNTARY_PAYMENT";
      execution.setVariable("collectionStrategy", strategy);
    }

    log.info("Case {} assessed, strategy: {}", caseId, strategy);
  }
}
