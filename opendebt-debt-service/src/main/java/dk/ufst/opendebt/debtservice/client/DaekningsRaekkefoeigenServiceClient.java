package dk.ufst.opendebt.debtservice.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import dk.ufst.opendebt.debtservice.service.FordringAllocation;

@Component
public class DaekningsRaekkefoeigenServiceClient {

  /**
   * Delegates allocation of the given amount to DaekningsRaekkefoeigenService for tier-2
   * gendækning. Returns list of fordring allocations. Stub implementation — integration to be added
   * later.
   */
  public List<FordringAllocation> allocate(UUID debtorPersonId, BigDecimal amount) {
    return List.of();
  }
}
