package dk.ufst.opendebt.debtservice.section50.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentCoverageOrderClient {
  List<String> orderPrincipalClaimIds(
      UUID debtorPersonId, BigDecimal availableAmount, List<String> candidateClaimIds);
}
