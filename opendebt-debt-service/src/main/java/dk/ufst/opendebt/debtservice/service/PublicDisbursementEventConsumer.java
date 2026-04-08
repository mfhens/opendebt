package dk.ufst.opendebt.debtservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for PublicDisbursementEvent — triggers FR-1 modregning workflow (SPEC-058 §3.1).
 *
 * <p>Listens for disbursement events and delegates to {@link ModregningService}.
 */
@Component
public class PublicDisbursementEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(PublicDisbursementEventConsumer.class);

  private final ModregningService modregningService;

  public PublicDisbursementEventConsumer(ModregningService modregningService) {
    this.modregningService = modregningService;
  }

  @EventListener
  public void onPublicDisbursementEvent(PublicDisbursementEvent event) {
    log.info(
        "Processing PublicDisbursementEvent for debtorPersonId={}, nemkontoRef={}",
        event.debtorPersonId(),
        event.nemkontoReferenceId());
    boolean restrictedPayment = PaymentType.BOERNE_OG_UNGEYDELSE.name().equals(event.paymentType());
    modregningService.initiateModregning(
        event.debtorPersonId(),
        event.disbursementAmount(),
        PaymentType.valueOf(event.paymentType()),
        event,
        restrictedPayment);
  }
}
