package dk.ufst.opendebt.debtservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;

import dk.ufst.opendebt.debtservice.service.ModregningDecisionKind;
import dk.ufst.opendebt.debtservice.service.ModregningReadModelService;
import dk.ufst.opendebt.debtservice.service.ModregningResult;
import dk.ufst.opendebt.debtservice.service.ModregningService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModregningController — CR-002 lineage read model")
class ModregningControllerTest {

  @Mock private ModregningService modregningService;
  @Mock private ModregningReadModelService modregningReadModelService;

  private ModregningController underTest;

  @BeforeEach
  void setUp() {
    underTest = new ModregningController(modregningService, modregningReadModelService);
  }

  @Test
  @DisplayName("GET returns operative lineage summary fields instead of raw event identity")
  void getModregningEvents_returnsOperativeLineageSummary() {
    UUID debtorId = UUID.randomUUID();
    when(modregningReadModelService.listOperativeEvents(eq(debtorId)))
        .thenReturn(
            List.of(
                new ModregningResult(
                    "DEC-NKR-5800-001-WAIVER",
                    "LIN-NKR-5800-001",
                    ModregningDecisionKind.SUPERSEDING_WAIVER_DECISION,
                    true,
                    "DEC-NKR-5800-001",
                    true,
                    UUID.randomUUID(),
                    debtorId,
                    LocalDate.of(2025, 3, 15),
                    new BigDecimal("1000.00"),
                    new BigDecimal("400.00"),
                    new BigDecimal("300.00"),
                    new BigDecimal("200.00"),
                    new BigDecimal("100.00"),
                    true,
                    false,
                    null,
                    LocalDate.of(2025, 6, 15),
                    null,
                    true,
                    List.of())));

    ResponseEntity<List<ModregningController.ModregningEventSummary>> response =
        underTest.getModregningEvents(debtorId);

    assertThat(response.getBody()).hasSize(1);
    ModregningController.ModregningEventSummary summary = response.getBody().get(0);
    assertThat(summary.decisionReference()).isEqualTo("DEC-NKR-5800-001-WAIVER");
    assertThat(summary.lineageReference()).isEqualTo("LIN-NKR-5800-001");
    assertThat(summary.decisionKind())
        .isEqualTo(ModregningDecisionKind.SUPERSEDING_WAIVER_DECISION);
    assertThat(summary.operative()).isTrue();
    assertThat(summary.supersedesDecisionReference()).isEqualTo("DEC-NKR-5800-001");
    assertThat(summary.hasHistory()).isTrue();
    assertThat(summary.totalOffsetAmount()).isEqualByComparingTo(new BigDecimal("900.00"));
  }

  @Test
  @DisplayName("POST waiver returns successor decision summary metadata")
  void postTier2Waiver_returnsSuccessorDecisionSummary() {
    UUID debtorId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();

    when(modregningService.applyTier2Waiver(eq(debtorId), eq(eventId), eq("reason"), any()))
        .thenReturn(
            new ModregningResult(
                "DEC-NKR-5801-001-WAIVER",
                "LIN-NKR-5801-001",
                ModregningDecisionKind.SUPERSEDING_WAIVER_DECISION,
                true,
                "DEC-NKR-5801-001",
                true,
                UUID.randomUUID(),
                debtorId,
                LocalDate.of(2025, 3, 15),
                new BigDecimal("1000.00"),
                new BigDecimal("400.00"),
                BigDecimal.ZERO,
                new BigDecimal("600.00"),
                BigDecimal.ZERO,
                true,
                false,
                null,
                LocalDate.of(2026, 3, 15),
                null,
                true,
                List.of()));

    ResponseEntity<ModregningController.ModregningEventSummary> response =
        underTest.postTier2Waiver(
            debtorId,
            eventId,
            new ModregningController.WaiverRequest("reason"),
            new TestingAuthenticationToken(UUID.randomUUID().toString(), "n/a"));

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().decisionReference()).isEqualTo("DEC-NKR-5801-001-WAIVER");
    assertThat(response.getBody().supersedesDecisionReference()).isEqualTo("DEC-NKR-5801-001");
    assertThat(response.getBody().decisionKind())
        .isEqualTo(ModregningDecisionKind.SUPERSEDING_WAIVER_DECISION);
  }
}
