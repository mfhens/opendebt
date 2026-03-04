package dk.ufst.opendebt.caseservice.workflow.delegates;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Delegate for checking if payment has been received. */
@Slf4j
@Component("checkPaymentDelegate")
@RequiredArgsConstructor
public class CheckPaymentDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) {
    String caseId = (String) execution.getVariable("caseId");
    log.info("Checking payment status for case: {}", caseId);

    // TODO: Call payment service to check payment status
    // PaymentStatus status = paymentServiceClient.getPaymentStatus(caseId);

    // For now, default to not paid
    boolean paid = false;
    boolean fullyPaid = false;

    execution.setVariable("paid", paid);
    execution.setVariable("fullyPaid", fullyPaid);

    log.info("Case {} payment check: paid={}, fullyPaid={}", caseId, paid, fullyPaid);
  }
}
