package dk.ufst.opendebt.caseservice.workflow.delegates;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Delegate for sending letters via the letter service. */
@Slf4j
@Component("sendLetterDelegate")
@RequiredArgsConstructor
public class SendLetterDelegate implements JavaDelegate {

  private Expression letterType;

  @Override
  public void execute(DelegateExecution execution) {
    String caseId = (String) execution.getVariable("caseId");
    String type = letterType != null ? (String) letterType.getValue(execution) : "UNKNOWN";

    log.info("Sending letter type {} for case: {}", type, caseId);

    // TODO: Integrate with the letter service when outbound dispatch is implemented.

    execution.setVariable("letterSent_" + type, true);
    log.info("Letter {} sent for case: {}", type, caseId);
  }
}
