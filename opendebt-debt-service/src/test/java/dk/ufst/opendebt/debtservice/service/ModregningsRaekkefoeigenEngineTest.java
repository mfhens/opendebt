package dk.ufst.opendebt.debtservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.debtservice.client.DaekningsRaekkefoeigenServiceClient;

/**
 * Unit tests for {@link ModregningsRaekkefoeigenEngine}.
 *
 * <p>Covers the 3-tier allocation logic per GIL § 7: tier-1 (paying authority), tier-2 (RIM
 * inddrivelse), and tier-3 (other fordringer), including the P057 delegation path and fallback.
 */
@ExtendWith(MockitoExtension.class)
class ModregningsRaekkefoeigenEngineTest {

  private static final UUID DEBTOR = UUID.randomUUID();
  private static final UUID ORG_A = UUID.randomUUID();
  private static final LocalDate TODAY = LocalDate.now();

  @Mock private DaekningsRaekkefoeigenServiceClient p057Client;
  @Mock private FordringQueryPort fordringQueryPort;

  private ModregningsRaekkefoeigenEngine engine;

  @BeforeEach
  void setUp() {
    engine = new ModregningsRaekkefoeigenEngine(p057Client, fordringQueryPort);
  }

  // ─── helpers ────────────────────────────────────────────────────────────────

  private FordringProjection proj(UUID id, String amount) {
    return new FordringProjection(id, new BigDecimal(amount), TODAY);
  }

  private BigDecimal bd(String val) {
    return new BigDecimal(val);
  }

  // ─── Tier 1 ─────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Tier-1 (paying authority)")
  class Tier1 {

    @Test
    @DisplayName("full absorption by tier-1 — no tier-2/3 allocated")
    void tier1AbsorbsAll() {
      UUID f1 = UUID.randomUUID();
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 1, ORG_A))
          .thenReturn(List.of(proj(f1, "500.00")));

      TierAllocationResult result = engine.allocate(DEBTOR, bd("300.00"), false, ORG_A);

      assertThat(result.tier1Allocations()).hasSize(1);
      assertThat(result.tier1Allocations().get(0).amountCovered()).isEqualByComparingTo("300.00");
      assertThat(result.tier2Allocations()).isEmpty();
      assertThat(result.tier3Allocations()).isEmpty();
      assertThat(result.residualPayoutAmount()).isEqualByComparingTo("0.00");
      verify(fordringQueryPort, never()).getActiveFordringer(DEBTOR, 2, null);
      verify(p057Client, never()).allocate(any(), any());
    }

    @Test
    @DisplayName("tier-1 exact match — residual is zero")
    void tier1ExactMatch() {
      UUID f1 = UUID.randomUUID();
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 1, ORG_A))
          .thenReturn(List.of(proj(f1, "200.00")));

      TierAllocationResult result = engine.allocate(DEBTOR, bd("200.00"), false, ORG_A);

      assertThat(result.tier1Allocations()).hasSize(1);
      assertThat(result.residualPayoutAmount()).isEqualByComparingTo("0.00");
      verify(fordringQueryPort, never()).getActiveFordringer(DEBTOR, 2, null);
    }
  }

  // ─── Tier 2 ─────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Tier-2 (RIM inddrivelse)")
  class Tier2 {

    @Test
    @DisplayName("full tier-2 coverage — P057 not called")
    void tier2FullCoverage() {
      UUID t2f = UUID.randomUUID();
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 1, null)).thenReturn(List.of());
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 2, null))
          .thenReturn(List.of(proj(t2f, "100.00")));

      TierAllocationResult result = engine.allocate(DEBTOR, bd("300.00"), false, null);

      assertThat(result.tier2Allocations()).hasSize(1);
      assertThat(result.tier2Allocations().get(0).amountCovered()).isEqualByComparingTo("100.00");
      assertThat(result.residualPayoutAmount()).isEqualByComparingTo("200.00");
      verify(p057Client, never()).allocate(any(), any());
    }

    @Test
    @DisplayName("partial tier-2 — delegates to P057")
    void tier2PartialDelegatesToP057() {
      UUID t2f1 = UUID.randomUUID();
      UUID t2f2 = UUID.randomUUID();
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 1, null)).thenReturn(List.of());
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 2, null))
          .thenReturn(List.of(proj(t2f1, "200.00"), proj(t2f2, "300.00")));
      when(p057Client.allocate(eq(DEBTOR), eq(bd("150.00"))))
          .thenReturn(List.of(new FordringAllocation(t2f1, bd("150.00"), 2)));

      TierAllocationResult result = engine.allocate(DEBTOR, bd("150.00"), false, null);

      assertThat(result.tier2Allocations()).hasSize(1);
      assertThat(result.tier2Allocations().get(0).amountCovered()).isEqualByComparingTo("150.00");
      assertThat(result.residualPayoutAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("partial tier-2 — P057 returns empty, fallback to first fordring")
    void tier2PartialFallbackToFirstFordring() {
      UUID t2f1 = UUID.randomUUID();
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 1, null)).thenReturn(List.of());
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 2, null))
          .thenReturn(List.of(proj(t2f1, "400.00"), proj(UUID.randomUUID(), "200.00")));
      when(p057Client.allocate(any(), any())).thenReturn(List.of());

      TierAllocationResult result = engine.allocate(DEBTOR, bd("100.00"), false, null);

      assertThat(result.tier2Allocations()).hasSize(1);
      assertThat(result.tier2Allocations().get(0).fordringId()).isEqualTo(t2f1);
      assertThat(result.tier2Allocations().get(0).amountCovered()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("skipTier2=true — tier-2 not allocated, P057 not called")
    void skipTier2() {
      UUID t3f = UUID.randomUUID();
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 1, null)).thenReturn(List.of());
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 3, null))
          .thenReturn(List.of(proj(t3f, "500.00")));

      TierAllocationResult result = engine.allocate(DEBTOR, bd("200.00"), true, null);

      assertThat(result.tier2Allocations()).isEmpty();
      assertThat(result.tier3Allocations()).hasSize(1);
      assertThat(result.tier3Allocations().get(0).amountCovered()).isEqualByComparingTo("200.00");
      verify(fordringQueryPort, never()).getActiveFordringer(DEBTOR, 2, null);
      verify(p057Client, never()).allocate(any(), any());
    }
  }

  // ─── Tier 3 ─────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Tier-3 (other fordringer)")
  class Tier3 {

    @Test
    @DisplayName("tier-3 absorbs residual after tier-1 and tier-2")
    void tier3AbsorbsResidual() {
      UUID t1f = UUID.randomUUID();
      UUID t2f = UUID.randomUUID();
      UUID t3f = UUID.randomUUID();
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 1, null))
          .thenReturn(List.of(proj(t1f, "100.00")));
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 2, null))
          .thenReturn(List.of(proj(t2f, "100.00")));
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 3, null))
          .thenReturn(List.of(proj(t3f, "500.00")));

      TierAllocationResult result = engine.allocate(DEBTOR, bd("350.00"), false, null);

      assertThat(result.tier1Allocations().get(0).amountCovered()).isEqualByComparingTo("100.00");
      assertThat(result.tier2Allocations().get(0).amountCovered()).isEqualByComparingTo("100.00");
      assertThat(result.tier3Allocations().get(0).amountCovered()).isEqualByComparingTo("150.00");
      assertThat(result.residualPayoutAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("amount exceeds all tiers — residual returned")
    void residualWhenExceedsAllTiers() {
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 1, null))
          .thenReturn(List.of(proj(UUID.randomUUID(), "50.00")));
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 2, null))
          .thenReturn(List.of(proj(UUID.randomUUID(), "50.00")));
      when(fordringQueryPort.getActiveFordringer(DEBTOR, 3, null))
          .thenReturn(List.of(proj(UUID.randomUUID(), "50.00")));

      TierAllocationResult result = engine.allocate(DEBTOR, bd("300.00"), false, null);

      assertThat(result.residualPayoutAmount()).isEqualByComparingTo("150.00");
    }
  }
}
