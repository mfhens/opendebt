package dk.ufst.opendebt.caseservice.workflow.delegates;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Delegate for closing a case. */
@Slf4j
@Component("closeCaseDelegate")
@RequiredArgsConstructor
public class CloseCaseDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) {
    String caseId = (String) execution.getVariable("caseId");
    boolean paid = Boolean.TRUE.equals(execution.getVariable("fullyPaid"));

    log.info("Closing case: {}, paid: {}", caseId, paid);

    String closureStatus = paid ? "CLOSED_PAID" : "CLOSED_OTHER";
    execution.setVariable("closureStatus", closureStatus);

    // TODO: Update case status in database via case service
    // caseService.closeCase(caseId, closureStatus);

    log.info("Case {} closed with status: {}", caseId, closureStatus);
  }
}
