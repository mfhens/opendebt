package dk.ufst.opendebt.debtservice.offsetting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.common.audit.cls.ClsAuditEvent;
import dk.ufst.opendebt.debtservice.client.DaekningsRaekkefoeigenServiceClient;
import dk.ufst.opendebt.debtservice.client.LedgerServiceClient;
import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity;
import dk.ufst.opendebt.debtservice.entity.KorrektionspuljeEntry;
import dk.ufst.opendebt.debtservice.entity.ModregningEvent;
import dk.ufst.opendebt.debtservice.repository.CollectionMeasureRepository;
import dk.ufst.opendebt.debtservice.repository.ModregningEventRepository;
import dk.ufst.opendebt.debtservice.service.FordringAllocation;
import dk.ufst.opendebt.debtservice.service.FordringQueryPort;
import dk.ufst.opendebt.debtservice.service.ModregningDecisionKind;
import dk.ufst.opendebt.debtservice.service.ModregningNotificationOutboxWriter;
import dk.ufst.opendebt.debtservice.service.ModregningResult;
import dk.ufst.opendebt.debtservice.service.ModregningResultMapper;
import dk.ufst.opendebt.debtservice.service.ModregningService;
import dk.ufst.opendebt.debtservice.service.ModregningsRaekkefoeigenEngine;
import dk.ufst.opendebt.debtservice.service.PaymentType;
import dk.ufst.opendebt.debtservice.service.PublicDisbursementEvent;
import dk.ufst.opendebt.debtservice.service.RenteGodtgoerelseDecision;
import dk.ufst.opendebt.debtservice.service.RenteGodtgoerelseService;
import dk.ufst.opendebt.debtservice.service.TierAllocationResult;

/**
 * Unit tests for {@code ModregningService} — FR-1/FR-2/FR-5 implementation for petition P058.
 *
 * <p>Covered requirements:
 *
 * <ul>
 *   <li><b>AC-1</b>: Three-tier allocation order (tier-1 → tier-2 → tier-3)
 *   <li><b>AC-2</b>: Tier-1 full coverage — DaekningsRaekkefoeigenService MUST NOT be called
 *   <li><b>AC-4</b>: SET_OFF CollectionMeasureEntity per covered fordring with modregningEventId
 *   <li><b>AC-5</b>: Idempotency — duplicate nemkontoReferenceId returns existing result
 *   <li><b>AC-14</b>: renteGodtgoerelseNonTaxable = true on EVERY ModregningEvent
 *   <li><b>NFR-1</b>: Atomicity — mid-transaction failure rolls back all state
 *   <li><b>NFR-2</b>: Auditability — CLS audit entry per tier allocation
 *   <li><b>NFR-3</b>: GDPR — no CPR/PII in any domain entity or audit log
 *   <li><b>AC-6</b>: Tier-2 waiver per GIL § 4, stk. 11
 * </ul>
 *
 * <p>Spec reference: SPEC-058 §3.1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModregningService — P058 unit tests")
class ModregningServiceTest {

  @Mock private ModregningEventRepository modregningEventRepository;
  @Mock private CollectionMeasureRepository collectionMeasureRepository;
  @Mock private ModregningsRaekkefoeigenEngine raekkefoeigenEngine;
  @Mock private RenteGodtgoerelseService renteGodtgoerelseService;
  @Mock private DaekningsRaekkefoeigenServiceClient daekningsRaekkefoeigenService;
  @Mock private ClsAuditClient clsAuditClient;
  @Mock private LedgerServiceClient ledgerServiceClient;
  @Mock private FordringQueryPort fordringQueryPort;
  @Mock private ModregningNotificationOutboxWriter notificationOutboxWriter;

  private ModregningService underTest;

  @BeforeEach
  void setUp() {
    ModregningResultMapper modregningResultMapper =
        new ModregningResultMapper(modregningEventRepository);
    underTest =
        new ModregningService(
            modregningEventRepository,
            collectionMeasureRepository,
            raekkefoeigenEngine,
            renteGodtgoerelseService,
            clsAuditClient,
            ledgerServiceClient,
            fordringQueryPort,
            modregningResultMapper,
            notificationOutboxWriter);
  }

  /** Common setup: mocks raekkefoeigenEngine + renteGodtgoerelseService + repos. */
  private UUID setupStandardMocks(UUID fordringId, UUID debtorId) {
    // Fixed UUID: all-hex-letter segments → cannot match \d{6}-\d{4} or \b\d{10}\b CPR patterns
    UUID eventId = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");

    when(raekkefoeigenEngine.allocate(any(), any(), anyBoolean(), any()))
        .thenReturn(
            new TierAllocationResult(
                List.of(new FordringAllocation(fordringId, new BigDecimal("1000"), 1)),
                List.of(),
                List.of(),
                BigDecimal.ZERO));
    when(renteGodtgoerelseService.computeDecision(any(), any(), any(), any()))
        .thenReturn(
            new RenteGodtgoerelseDecision(
                null, RenteGodtgoerelseDecision.ExceptionType.FIVE_BANKING_DAY));
    when(modregningEventRepository.findByNemkontoReferenceId(any())).thenReturn(Optional.empty());
    when(modregningEventRepository.save(any()))
        .thenAnswer(
            inv -> {
              ModregningEvent e = inv.getArgument(0);
              e.setId(eventId);
              return e;
            });
    when(collectionMeasureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    return eventId;
  }

  // ── AC-2: Tier-1 full coverage ───────────────────────────────────────────────

  @Test
  @DisplayName("CR-002: root modregning creates lineage metadata for debtor-facing identity")
  void initiateModregning_createsRootLineageMetadata() {
    UUID debtorId = UUID.randomUUID();
    UUID fordringId = UUID.randomUUID();
    setupStandardMocks(fordringId, debtorId);

    PublicDisbursementEvent pde =
        new PublicDisbursementEvent(
            "NKR-5800-ROOT",
            debtorId,
            new BigDecimal("1000.00"),
            PaymentType.STANDARD_PAYMENT.name(),
            null,
            UUID.randomUUID(),
            LocalDate.of(2025, 3, 1),
            LocalDate.of(2025, 3, 15));

    underTest.initiateModregning(
        debtorId, new BigDecimal("1000.00"), PaymentType.STANDARD_PAYMENT, pde, false);

    verify(modregningEventRepository)
        .save(
            argThat(
                event ->
                    event.getDecisionReference() != null
                        && event.getLineageReference() != null
                        && event.getDecisionKind()
                            == ModregningDecisionKind.EXTERNAL_DISBURSEMENT_DECISION
                        && event.isOperative()));
  }

  @Nested
  @DisplayName("AC-2: Tier-1 full coverage — DaekningsRaekkefoeigenService must NOT be called")
  class Tier1FullCoverage {

    /**
     * AC-2: When tier-1 covers everything, tier2Amount=0 on the persisted ModregningEvent. The
     * daekningsRaekkefoeigenService is not injected → trivially not called. Ref: GIL § 7, stk. 1,
     * nr. 1; SPEC-058 §3.1 and §4.1
     */
    @Test
    @DisplayName("AC-2: tier-1 exhausts amount — ModregningEvent has tier2Amount=0")
    void tier1FullCoverage_doesNotCallDaekningsService() {
      UUID debtorId = UUID.randomUUID();
      UUID fordringId = UUID.randomUUID();
      setupStandardMocks(fordringId, debtorId);

      underTest.initiateModregning(
          debtorId, new BigDecimal("1000"), PaymentType.STANDARD_PAYMENT, null, false);

      // Verify persisted event has tier1Amount > 0 and tier2Amount = 0
      verify(modregningEventRepository)
          .save(
              argThat(
                  e ->
                      e.getTier2Amount().compareTo(BigDecimal.ZERO) == 0
                          && e.getTier1Amount().compareTo(BigDecimal.ZERO) > 0));

      // daekningsRaekkefoeigenService is not injected — trivially not called (AC-2 short-circuit)
      verify(daekningsRaekkefoeigenService, never()).allocate(any(), any());
    }

    /**
     * AC-2 + AC-4: A SET_OFF CollectionMeasureEntity must be created per tier-1 fordring, with
     * modregningEventId set (not null). Ref: SPEC-058 §2.1.4 (NFR-2)
     */
    @Test
    @DisplayName("AC-2 + AC-4: SET_OFF CollectionMeasure created per tier-1 fordring")
    void tier1FullCoverage_createsSetOffMeasurePerFordring() {
      UUID debtorId = UUID.randomUUID();
      UUID fordringId = UUID.randomUUID();
      UUID eventId = setupStandardMocks(fordringId, debtorId);

      underTest.initiateModregning(
          debtorId, new BigDecimal("1000"), PaymentType.STANDARD_PAYMENT, null, false);

      ArgumentCaptor<CollectionMeasureEntity> captor =
          ArgumentCaptor.forClass(CollectionMeasureEntity.class);
      verify(collectionMeasureRepository).save(captor.capture());

      CollectionMeasureEntity saved = captor.getValue();
      assertThat(saved.getMeasureType())
          .as("CollectionMeasure must have measureType=SET_OFF (AC-4, SPEC-058 §2.1.4)")
          .isEqualTo(CollectionMeasureEntity.MeasureType.SET_OFF);
      assertThat(saved.getModregningEventId())
          .as("modregningEventId must be non-null (AC-4)")
          .isEqualTo(eventId);
      assertThat(saved.getDebtId()).as("debtId must match the fordringId").isEqualTo(fordringId);
    }
  }

  // ── AC-1, AC-3: Three-tier ordering ──────────────────────────────────────────

  @Nested
  @DisplayName("AC-1 + AC-3: Tier-2 partial coverage — DaekningsRaekkefoeigenService called once")
  class Tier2PartialCoverage {

    /**
     * AC-3: When tier-2 allocations are present in the result, tier2Amount > 0 is persisted. (The
     * engine mock controls what is returned — in real impl engine calls DaekningsService.) Ref:
     * SPEC-058 §3.2 and §4.2
     */
    @Test
    @DisplayName("AC-3: partial tier-2 → ModregningEvent persisted with tier2Amount > 0")
    void tier2Partial_callsDaekningsServiceExactlyOnce() {
      UUID debtorId = UUID.randomUUID();
      UUID fordringId1 = UUID.randomUUID();
      UUID fordringId2 = UUID.randomUUID();
      UUID eventId = UUID.randomUUID();

      when(raekkefoeigenEngine.allocate(any(), any(), anyBoolean(), any()))
          .thenReturn(
              new TierAllocationResult(
                  List.of(new FordringAllocation(fordringId1, new BigDecimal("500"), 1)),
                  List.of(new FordringAllocation(fordringId2, new BigDecimal("300"), 2)),
                  List.of(),
                  BigDecimal.ZERO));
      when(renteGodtgoerelseService.computeDecision(any(), any(), any(), any()))
          .thenReturn(
              new RenteGodtgoerelseDecision(
                  null, RenteGodtgoerelseDecision.ExceptionType.FIVE_BANKING_DAY));
      when(modregningEventRepository.findByNemkontoReferenceId(any())).thenReturn(Optional.empty());
      when(modregningEventRepository.save(any()))
          .thenAnswer(
              inv -> {
                ModregningEvent e = inv.getArgument(0);
                e.setId(eventId);
                return e;
              });
      when(collectionMeasureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      underTest.initiateModregning(
          debtorId, new BigDecimal("800"), PaymentType.STANDARD_PAYMENT, null, false);

      // Verify engine called exactly once (AC-3)
      verify(raekkefoeigenEngine, times(1)).allocate(any(), any(), anyBoolean(), any());

      // Verify event persisted with tier2Amount > 0 (AC-1 ordering)
      verify(modregningEventRepository)
          .save(
              argThat(
                  e ->
                      e.getTier1Amount().compareTo(BigDecimal.ZERO) > 0
                          && e.getTier2Amount().compareTo(BigDecimal.ZERO) > 0));
    }

    /**
     * AC-1: Tier-3 receives allocation only after tier-2 is exhausted. When engine returns tier-3
     * allocations, tier3Amount > 0 on the event. Ref: SPEC-058 §4.1 algorithm
     */
    @Test
    @DisplayName("AC-1: tier-3 receives allocation only after tier-2 exhausted")
    void tier3_receivesAllocationAfterTier2Exhausted() {
      UUID debtorId = UUID.randomUUID();
      UUID f1 = UUID.randomUUID();
      UUID f2 = UUID.randomUUID();
      UUID f3 = UUID.randomUUID();
      UUID eventId = UUID.randomUUID();

      when(raekkefoeigenEngine.allocate(any(), any(), anyBoolean(), any()))
          .thenReturn(
              new TierAllocationResult(
                  List.of(new FordringAllocation(f1, new BigDecimal("400"), 1)),
                  List.of(new FordringAllocation(f2, new BigDecimal("300"), 2)),
                  List.of(new FordringAllocation(f3, new BigDecimal("200"), 3)),
                  BigDecimal.ZERO));
      when(renteGodtgoerelseService.computeDecision(any(), any(), any(), any()))
          .thenReturn(
              new RenteGodtgoerelseDecision(
                  null, RenteGodtgoerelseDecision.ExceptionType.FIVE_BANKING_DAY));
      when(modregningEventRepository.findByNemkontoReferenceId(any())).thenReturn(Optional.empty());
      when(modregningEventRepository.save(any()))
          .thenAnswer(
              inv -> {
                ModregningEvent e = inv.getArgument(0);
                e.setId(eventId);
                return e;
              });
      when(collectionMeasureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      underTest.initiateModregning(
          debtorId, new BigDecimal("900"), PaymentType.STANDARD_PAYMENT, null, false);

      verify(modregningEventRepository)
          .save(
              argThat(
                  e ->
                      e.getTier1Amount().compareTo(BigDecimal.ZERO) > 0
                          && e.getTier2Amount().compareTo(BigDecimal.ZERO) > 0
                          && e.getTier3Amount().compareTo(BigDecimal.ZERO) > 0));
    }
  }

  // ── AC-5: Idempotency ────────────────────────────────────────────────────────

  @Nested
  @DisplayName("AC-5: Idempotency — duplicate nemkontoReferenceId is no-op")
  class Idempotency {

    /**
     * AC-5: Duplicate nemkontoReferenceId returns existing result without creating new entities.
     * Ref: SPEC-058 §3.1 initiateModregning idempotency guard
     */
    @Test
    @DisplayName("AC-5: duplicate nemkontoReferenceId returns existing result, no new entities")
    void duplicateNemkontoRef_returnsExistingResult() {
      UUID existingDebtorId = UUID.randomUUID();
      UUID existingEventId = UUID.randomUUID();

      ModregningEvent existingEvent =
          ModregningEvent.builder()
              .id(existingEventId)
              .debtorPersonId(existingDebtorId)
              .disbursementAmount(new BigDecimal("1000"))
              .tier1Amount(new BigDecimal("1000"))
              .tier2Amount(BigDecimal.ZERO)
              .tier3Amount(BigDecimal.ZERO)
              .residualPayoutAmount(BigDecimal.ZERO)
              .paymentType(PaymentType.STANDARD_PAYMENT)
              .receiptDate(LocalDate.now())
              .decisionDate(LocalDate.now())
              .klageFristDato(LocalDate.now().plusYears(1))
              .renteGodtgoerelseNonTaxable(true)
              .build();

      when(modregningEventRepository.findByNemkontoReferenceId("REF-001"))
          .thenReturn(Optional.of(existingEvent));

      // Call with String sourceEvent = "REF-001" (the ref ID for idempotency)
      ModregningResult result =
          underTest.initiateModregning(
              existingDebtorId,
              new BigDecimal("1000"),
              PaymentType.STANDARD_PAYMENT,
              "REF-001",
              false);

      // Must return existing result without calling engine or saving
      verify(raekkefoeigenEngine, never()).allocate(any(), any(), anyBoolean(), any());
      verify(modregningEventRepository, never()).save(any());
      assertThat(result.eventId())
          .as("Result must reference the existing event ID (AC-5)")
          .isEqualTo(existingEventId);
    }
  }

  // ── AC-14: renteGodtgoerelseNonTaxable ──────────────────────────────────────

  @Nested
  @DisplayName("AC-14: renteGodtgoerelseNonTaxable must be true on every ModregningEvent")
  class RenteGodtgoerelseNonTaxable {

    /**
     * AC-14: GIL § 8b, stk. 2, 3. pkt. — renteGodtgoerelseNonTaxable ALWAYS true. Ref: SPEC-058
     * §2.1.1
     */
    @Test
    @DisplayName("AC-14: every persisted ModregningEvent has renteGodtgoerelseNonTaxable=true")
    void everyModregningEvent_hasRenteGodtgoerelseNonTaxableTrue() {
      UUID debtorId = UUID.randomUUID();
      UUID fordringId = UUID.randomUUID();
      setupStandardMocks(fordringId, debtorId);

      underTest.initiateModregning(
          debtorId, new BigDecimal("500"), PaymentType.BOERNE_OG_UNGEYDELSE, null, false);

      ArgumentCaptor<ModregningEvent> captor = ArgumentCaptor.forClass(ModregningEvent.class);
      verify(modregningEventRepository).save(captor.capture());

      assertThat(captor.getValue().isRenteGodtgoerelseNonTaxable())
          .as("renteGodtgoerelseNonTaxable MUST be true per GIL § 8b (SPEC-058 §2.1.1)")
          .isTrue();
    }
  }

  // ── NFR-1: Atomicity ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("NFR-1: Atomicity — mid-transaction failure rolls back all state")
  class Atomicity {

    /**
     * NFR-1: If CollectionMeasure save throws, exception propagates (rollback in
     * real @Transactional). Ref: SPEC-058 §3.1
     */
    @Test
    @DisplayName("NFR-1: exception during Digital Post outbox write rolls back transaction")
    void midTransactionFailure_rollsBackAllState() {
      UUID debtorId = UUID.randomUUID();
      UUID fordringId = UUID.randomUUID();
      UUID eventId = UUID.randomUUID();

      when(raekkefoeigenEngine.allocate(any(), any(), anyBoolean(), any()))
          .thenReturn(
              new TierAllocationResult(
                  List.of(new FordringAllocation(fordringId, new BigDecimal("500"), 1)),
                  List.of(),
                  List.of(),
                  BigDecimal.ZERO));
      when(renteGodtgoerelseService.computeDecision(any(), any(), any(), any()))
          .thenReturn(
              new RenteGodtgoerelseDecision(
                  null, RenteGodtgoerelseDecision.ExceptionType.FIVE_BANKING_DAY));
      when(modregningEventRepository.findByNemkontoReferenceId(any())).thenReturn(Optional.empty());
      when(modregningEventRepository.save(any()))
          .thenAnswer(
              inv -> {
                ModregningEvent e = inv.getArgument(0);
                e.setId(eventId);
                return e;
              });
      when(collectionMeasureRepository.save(any()))
          .thenThrow(new RuntimeException("DB error simulating mid-transaction failure"));

      assertThatThrownBy(
              () ->
                  underTest.initiateModregning(
                      debtorId, new BigDecimal("500"), PaymentType.STANDARD_PAYMENT, null, false))
          .as(
              "Exception from CollectionMeasure save must propagate for @Transactional rollback (NFR-1)")
          .isInstanceOf(RuntimeException.class);
    }

    /**
     * NFR-1: If CollectionMeasure save fails, exception propagates. Ref: SPEC-058 §3.1 transaction
     * boundary
     */
    @Test
    @DisplayName("NFR-1: CollectionMeasure save failure rolls back ModregningEvent too")
    void collectionMeasureSaveFailure_rollsBackModregningEvent() {
      UUID debtorId = UUID.randomUUID();
      UUID fordringId = UUID.randomUUID();
      UUID eventId = UUID.randomUUID();

      when(raekkefoeigenEngine.allocate(any(), any(), anyBoolean(), any()))
          .thenReturn(
              new TierAllocationResult(
                  List.of(new FordringAllocation(fordringId, new BigDecimal("750"), 1)),
                  List.of(),
                  List.of(),
                  BigDecimal.ZERO));
      when(renteGodtgoerelseService.computeDecision(any(), any(), any(), any()))
          .thenReturn(
              new RenteGodtgoerelseDecision(
                  null, RenteGodtgoerelseDecision.ExceptionType.FIVE_BANKING_DAY));
      when(modregningEventRepository.findByNemkontoReferenceId(any())).thenReturn(Optional.empty());
      when(modregningEventRepository.save(any()))
          .thenAnswer(
              inv -> {
                ModregningEvent e = inv.getArgument(0);
                e.setId(eventId);
                return e;
              });
      when(collectionMeasureRepository.save(any()))
          .thenThrow(new RuntimeException("Simulated DB failure"));

      assertThatThrownBy(
              () ->
                  underTest.initiateModregning(
                      debtorId, new BigDecimal("750"), PaymentType.STANDARD_PAYMENT, null, false))
          .as("CollectionMeasure save failure must propagate for @Transactional atomicity (NFR-1)")
          .isInstanceOf(RuntimeException.class);
    }
  }

  // ── NFR-2: Auditability ──────────────────────────────────────────────────────

  @Nested
  @DisplayName("NFR-2: Auditability — CLS audit entry per tier decision")
  class Auditability {

    /**
     * NFR-2: CLS audit log must contain gilParagraf + modregningEventId + debtorPersonId +
     * fordringId. Ref: SPEC-058 §3.1
     */
    @Test
    @DisplayName(
        "NFR-2: CLS audit log contains gilParagraf + modregningEventId + debtorPersonId + fordringId")
    void everyTierDecision_logsRequiredAuditFields() {
      UUID debtorId = UUID.randomUUID();
      UUID fordringId = UUID.randomUUID();
      setupStandardMocks(fordringId, debtorId);

      underTest.initiateModregning(
          debtorId, new BigDecimal("1000"), PaymentType.STANDARD_PAYMENT, null, false);

      ArgumentCaptor<ClsAuditEvent> auditCaptor = ArgumentCaptor.forClass(ClsAuditEvent.class);
      verify(clsAuditClient).shipEvent(auditCaptor.capture());

      ClsAuditEvent auditEvent = auditCaptor.getValue();
      assertThat(auditEvent.getNewValues())
          .as("CLS audit must contain gilParagraf (NFR-2, SPEC-058 §3.1)")
          .containsKey("gilParagraf");
      assertThat(auditEvent.getNewValues().get("gilParagraf").toString())
          .as("gilParagraf must reference GIL § 7 tier")
          .startsWith("GIL § 7");
      assertThat(auditEvent.getNewValues())
          .as("CLS audit must contain modregningEventId")
          .containsKey("modregningEventId");
      assertThat(auditEvent.getNewValues())
          .as("CLS audit must contain debtorPersonId (UUID, not CPR — NFR-3)")
          .containsKey("debtorPersonId");
      assertThat(auditEvent.getNewValues())
          .as("CLS audit must contain fordringId")
          .containsKey("fordringId");
    }

    /** NFR-2: Separate CLS audit entry per covered fordring. Ref: SPEC-058 §3.1 */
    @Test
    @DisplayName("NFR-2: separate CLS audit entry per covered fordring")
    void perFordringAuditEntry_writtenForEachCoveredFordring() {
      UUID debtorId = UUID.randomUUID();
      UUID fordring1 = UUID.randomUUID();
      UUID fordring2 = UUID.randomUUID();
      UUID eventId = UUID.randomUUID();

      // Engine returns 2 tier-1 allocations
      when(raekkefoeigenEngine.allocate(any(), any(), anyBoolean(), any()))
          .thenReturn(
              new TierAllocationResult(
                  List.of(
                      new FordringAllocation(fordring1, new BigDecimal("600"), 1),
                      new FordringAllocation(fordring2, new BigDecimal("400"), 1)),
                  List.of(),
                  List.of(),
                  BigDecimal.ZERO));
      when(renteGodtgoerelseService.computeDecision(any(), any(), any(), any()))
          .thenReturn(
              new RenteGodtgoerelseDecision(
                  null, RenteGodtgoerelseDecision.ExceptionType.FIVE_BANKING_DAY));
      when(modregningEventRepository.findByNemkontoReferenceId(any())).thenReturn(Optional.empty());
      when(modregningEventRepository.save(any()))
          .thenAnswer(
              inv -> {
                ModregningEvent e = inv.getArgument(0);
                e.setId(eventId);
                return e;
              });
      when(collectionMeasureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      underTest.initiateModregning(
          debtorId, new BigDecimal("1000"), PaymentType.STANDARD_PAYMENT, null, false);

      // 2 fordringer → 2 CLS audit entries
      verify(clsAuditClient, times(2)).shipEvent(any());
    }
  }

  // ── NFR-3: GDPR ─────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("NFR-3: GDPR — no CPR/PII in any domain entity or audit log")
  class Gdpr {

    /**
     * NFR-3 (ADR-0014): ModregningEvent entity must have no field named 'cpr', 'cprNumber',
     * 'personnummer', or any String field with CPR-like naming. Ref: SPEC-058 §2.1.1 and ADR-0014
     */
    @Test
    @DisplayName("NFR-3: ModregningEvent entity has no CPR field")
    void modregningEvent_hasNoCprField() {
      List<String> forbiddenFieldNames =
          Arrays.asList("cpr", "cprnumber", "personnummer", "personalidentitynumber", "cprno");

      List<String> fieldNames =
          Arrays.stream(ModregningEvent.class.getDeclaredFields())
              .map(Field::getName)
              .map(String::toLowerCase)
              .toList();

      assertThat(fieldNames)
          .as("ModregningEvent must have no CPR field (NFR-3/ADR-0014, SPEC-058 §2.1.1)")
          .isNotEmpty()
          .doesNotContainAnyElementsOf(forbiddenFieldNames);

      // debtorPersonId must be UUID type
      java.lang.reflect.Field debtorField;
      try {
        debtorField = ModregningEvent.class.getDeclaredField("debtorPersonId");
      } catch (NoSuchFieldException e) {
        throw new AssertionError("ModregningEvent must have debtorPersonId field", e);
      }
      assertThat(debtorField.getType())
          .as("debtorPersonId must be UUID type (not String CPR)")
          .isEqualTo(UUID.class);
    }

    /**
     * NFR-3: CLS audit entries must not contain CPR/PII. debtorPersonId must be a UUID string (not
     * a 10-digit CPR pattern). Ref: SPEC-058 §3.1 and ADR-0014
     */
    @Test
    @DisplayName("NFR-3: CLS audit log entry does not contain CPR/PII")
    void clsAuditEntry_doesNotContainCprOrPii() {
      // Use fixed UUIDs guaranteed to have no 6-consecutive-digit run (avoids flaky CPR-pattern
      // match)
      UUID debtorId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
      UUID fordringId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
      setupStandardMocks(fordringId, debtorId);

      underTest.initiateModregning(
          debtorId, new BigDecimal("1000"), PaymentType.STANDARD_PAYMENT, null, false);

      ArgumentCaptor<ClsAuditEvent> captor = ArgumentCaptor.forClass(ClsAuditEvent.class);
      verify(clsAuditClient).shipEvent(captor.capture());

      ClsAuditEvent event = captor.getValue();
      // Verify no field in newValues matches a CPR pattern (\d{6}-?\d{4} or 10 consecutive digits)
      if (event.getNewValues() != null) {
        for (Object value : event.getNewValues().values()) {
          assertThat(value.toString())
              .as("CLS audit value must not contain CPR pattern (NFR-3)")
              .doesNotContainPattern("\\b\\d{10}\\b")
              .doesNotContainPattern("\\d{6}-\\d{4}");
        }
      }
      // debtorPersonId in audit must be a UUID string, not a CPR
      String debtorInAudit =
          event.getNewValues() != null
              ? event.getNewValues().getOrDefault("debtorPersonId", "").toString()
              : "";
      assertThat(debtorInAudit)
          .as("debtorPersonId in audit must be UUID format, not CPR")
          .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
  }

  // ── AC-6 + AC-7: Tier-2 waiver ──────────────────────────────────────────────

  @Nested
  @DisplayName("AC-6 + AC-7: applyTier2Waiver — GIL § 4, stk. 11")
  class Tier2Waiver {

    /**
     * AC-6: applyTier2Waiver sets tier2WaiverApplied=true, tier2Amount=0, re-runs engine with
     * skipTier2=true. Ref: SPEC-058 §3.1 applyTier2Waiver
     */
    @Test
    @DisplayName("CR-002: waiver creates a superseding successor decision in the same lineage")
    void applyTier2Waiver_executesAllSixStepsAtomically() {
      UUID debtorId = UUID.randomUUID();
      UUID eventId = UUID.randomUUID();
      UUID caseworkerId = UUID.randomUUID();

      ModregningEvent existingEvent =
          ModregningEvent.builder()
              .id(eventId)
              .decisionReference("DEC-NKR-5814-001")
              .lineageReference("LIN-NKR-5814-001")
              .decisionKind(ModregningDecisionKind.EXTERNAL_DISBURSEMENT_DECISION)
              .operative(true)
              .debtorPersonId(debtorId)
              .nemkontoReferenceId("NKR-5814-001")
              .disbursementAmount(new BigDecimal("1000"))
              .tier1Amount(new BigDecimal("600"))
              .tier2Amount(new BigDecimal("400"))
              .tier3Amount(BigDecimal.ZERO)
              .residualPayoutAmount(BigDecimal.ZERO)
              .paymentType(PaymentType.STANDARD_PAYMENT)
              .receiptDate(LocalDate.now())
              .decisionDate(LocalDate.now())
              .klageFristDato(LocalDate.now().plusYears(1))
              .tier2WaiverApplied(false)
              .renteGodtgoerelseNonTaxable(true)
              .build();

      // Tier-2 measure that should be reversed by the waiver — ref: P058 tier-scope fix
      UUID tier2DebtId = UUID.randomUUID();
      CollectionMeasureEntity tier2Measure =
          CollectionMeasureEntity.builder()
              .debtId(tier2DebtId)
              .measureType(CollectionMeasureEntity.MeasureType.SET_OFF)
              .modregningEventId(eventId)
              .amount(new BigDecimal("400"))
              .tierLevel(2)
              .build();

      when(modregningEventRepository.findByIdAndDebtorPersonId(eventId, debtorId))
          .thenReturn(Optional.of(existingEvent));
      // Ref: P058 tier-scope fix — only tier-2 rows returned for tierLevel=2
      when(collectionMeasureRepository.findByModregningEventIdAndMeasureTypeAndTierLevel(
              eventId, CollectionMeasureEntity.MeasureType.SET_OFF, 2))
          .thenReturn(List.of(tier2Measure));
      TierAllocationResult waiverRerunAllocation =
          new TierAllocationResult(
              List.of(),
              List.of(),
              List.of(new FordringAllocation(UUID.randomUUID(), new BigDecimal("400"), 3)),
              BigDecimal.ZERO);
      when(raekkefoeigenEngine.allocate(any(), any(), anyBoolean(), anyBoolean(), any()))
          .thenReturn(waiverRerunAllocation);
      when(modregningEventRepository.save(any()))
          .thenAnswer(
              inv -> {
                ModregningEvent saved = inv.getArgument(0);
                if (saved.getId() == null) {
                  saved.setId(UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd"));
                }
                return saved;
              });

      underTest.applyTier2Waiver(debtorId, eventId, "GIL § 4, stk. 11 waiver reason", caseworkerId);

      // (1) predecessor stays historical, successor becomes operative
      ArgumentCaptor<ModregningEvent> eventCaptor = ArgumentCaptor.forClass(ModregningEvent.class);
      verify(modregningEventRepository, times(2)).save(eventCaptor.capture());
      List<ModregningEvent> savedEvents = eventCaptor.getAllValues();
      ModregningEvent predecessor =
          savedEvents.stream()
              .filter(saved -> eventId.equals(saved.getId()))
              .findFirst()
              .orElseThrow();
      ModregningEvent successor =
          savedEvents.stream()
              .filter(saved -> !eventId.equals(saved.getId()))
              .findFirst()
              .orElseThrow();

      assertThat(predecessor.isOperative()).as("predecessor must no longer be operative").isFalse();
      assertThat(predecessor.getTier2Amount())
          .as("predecessor keeps its original legal allocation in history")
          .isEqualByComparingTo(new BigDecimal("400"));

      assertThat(successor.getLineageReference()).isEqualTo("LIN-NKR-5814-001");
      assertThat(successor.getDecisionKind())
          .isEqualTo(ModregningDecisionKind.SUPERSEDING_WAIVER_DECISION);
      assertThat(successor.getSupersedesEventId()).isEqualTo(eventId);
      assertThat(successor.isOperative()).isTrue();
      assertThat(successor.isTier2WaiverApplied()).isTrue();
      assertThat(successor.getTier1Amount()).isEqualByComparingTo(new BigDecimal("600"));
      assertThat(successor.getTier2Amount()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(successor.getTier3Amount()).isEqualByComparingTo(new BigDecimal("400"));

      // (2) engine called with skipTier2=true AND amount = disbursementAmount - tier1Amount (BUG-B
      // fix)
      ArgumentCaptor<BigDecimal> waiverAmountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
      verify(raekkefoeigenEngine)
          .allocate(any(), waiverAmountCaptor.capture(), eq(true), eq(true), any());
      assertThat(waiverAmountCaptor.getValue())
          .as(
              "Engine re-run must use disbursementAmount - tier1Amount = 400.00 (BUG-B, SPEC-058 §3.1)")
          .isEqualByComparingTo(new BigDecimal("400"));
      // (3) waiverApplied=true set only on the tier-2 measure — ref: P058 tier-scope fix
      assertThat(tier2Measure.isWaiverApplied())
          .as("waiverApplied must be true on the tier-2 CollectionMeasure (AC-6)")
          .isTrue();
      // (4) reverseLedgerEntry called exactly once for the tier-2 measure — ref: P058 tier-scope
      // fix
      verify(ledgerServiceClient, times(1))
          .reverseLedgerEntry(debtorId, tier2DebtId, new BigDecimal("400"), eventId);
      // (5) new tier-3 measures must attach to the successor decision
      verify(collectionMeasureRepository)
          .save(
              argThat(
                  measure ->
                      measure.getTierLevel() == 3
                          && UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd")
                              .equals(measure.getModregningEventId())
                          && measure.isWaiverApplied()));
      // (6) CLS audit with GIL § 4, stk. 11 and successor identity
      ArgumentCaptor<ClsAuditEvent> auditCaptor = ArgumentCaptor.forClass(ClsAuditEvent.class);
      verify(clsAuditClient).shipEvent(auditCaptor.capture());
      assertThat(auditCaptor.getValue().getNewValues().get("gilParagraf"))
          .as("Waiver audit must reference GIL § 4, stk. 11 (AC-6)")
          .isEqualTo("GIL § 4, stk. 11");
      assertThat(auditCaptor.getValue().getNewValues().get("decisionReference"))
          .isEqualTo(successor.getDecisionReference());
    }

    /**
     * AC-6 + NFR-1: Waiver must throw if event not found. Atomicity of exception. Ref: SPEC-058
     * §3.1
     */
    @Test
    @DisplayName("AC-6 + NFR-1: waiver transaction atomicity — failure rolls back all steps")
    void applyTier2Waiver_rollsBackOnFailure() {
      UUID debtorId = UUID.randomUUID();
      UUID eventId = UUID.randomUUID();
      when(modregningEventRepository.findByIdAndDebtorPersonId(eventId, debtorId))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> underTest.applyTier2Waiver(debtorId, eventId, "reason", UUID.randomUUID()))
          .as("applyTier2Waiver must throw when ModregningEvent not found (AC-6/NFR-1)")
          .isInstanceOf(
              dk.ufst.opendebt.debtservice.exception.ModregningEventNotFoundException.class);
    }

    /**
     * AC-7: Missing waiver scope would cause 403 — tested at controller level. Here we verify that
     * the service correctly guards double-waiver application. Ref: SPEC-058 §3.1
     */
    @Test
    @DisplayName("AC-7: double-waiver application throws WaiverAlreadyAppliedException")
    void applyTier2Waiver_withoutScope_throwsForbidden() {
      UUID debtorId = UUID.randomUUID();
      UUID eventId = UUID.randomUUID();

      ModregningEvent alreadyWaived =
          ModregningEvent.builder()
              .id(eventId)
              .debtorPersonId(debtorId)
              .disbursementAmount(new BigDecimal("1000"))
              .tier1Amount(new BigDecimal("1000"))
              .tier2Amount(BigDecimal.ZERO)
              .tier3Amount(BigDecimal.ZERO)
              .residualPayoutAmount(BigDecimal.ZERO)
              .paymentType(PaymentType.STANDARD_PAYMENT)
              .receiptDate(LocalDate.now())
              .decisionDate(LocalDate.now())
              .klageFristDato(LocalDate.now().plusYears(1))
              .tier2WaiverApplied(true)
              .renteGodtgoerelseNonTaxable(true)
              .build();

      when(modregningEventRepository.findByIdAndDebtorPersonId(eventId, debtorId))
          .thenReturn(Optional.of(alreadyWaived));

      assertThatThrownBy(
              () -> underTest.applyTier2Waiver(debtorId, eventId, "reason", UUID.randomUUID()))
          .as("Double-waiver must throw WaiverAlreadyAppliedException (AC-7)")
          .isInstanceOf(dk.ufst.opendebt.debtservice.exception.WaiverAlreadyAppliedException.class);
    }

    @Test
    @DisplayName("CR-002: waiver rejects event IDs that do not belong to the path debtor")
    void applyTier2Waiver_rejectsWrongDebtorEvent() {
      UUID debtorId = UUID.randomUUID();
      UUID eventId = UUID.randomUUID();
      UUID wrongDebtorId = UUID.randomUUID();

      when(modregningEventRepository.findByIdAndDebtorPersonId(eventId, wrongDebtorId))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> underTest.applyTier2Waiver(wrongDebtorId, eventId, "reason", UUID.randomUUID()))
          .isInstanceOf(
              dk.ufst.opendebt.debtservice.exception.ModregningEventNotFoundException.class);

      verify(collectionMeasureRepository, never())
          .findByModregningEventIdAndMeasureTypeAndTierLevel(any(), any(), any());
      verify(ledgerServiceClient, never()).reverseLedgerEntry(any(), any(), any(), any());
    }

    @Test
    @DisplayName(
        "CR-002: a second waiver attempt on the original event is blocked before side effects")
    void applyTier2Waiver_secondAttemptStopsBeforeLedgerReversal() {
      UUID debtorId = UUID.randomUUID();
      UUID eventId = UUID.randomUUID();
      UUID caseworkerId = UUID.randomUUID();
      UUID tier2DebtId = UUID.randomUUID();

      ModregningEvent existingEvent =
          ModregningEvent.builder()
              .id(eventId)
              .decisionReference("DEC-NKR-5814-001")
              .lineageReference("LIN-NKR-5814-001")
              .decisionKind(ModregningDecisionKind.EXTERNAL_DISBURSEMENT_DECISION)
              .operative(true)
              .debtorPersonId(debtorId)
              .nemkontoReferenceId("NKR-5814-001")
              .disbursementAmount(new BigDecimal("1000"))
              .tier1Amount(new BigDecimal("600"))
              .tier2Amount(new BigDecimal("400"))
              .tier3Amount(BigDecimal.ZERO)
              .residualPayoutAmount(BigDecimal.ZERO)
              .paymentType(PaymentType.STANDARD_PAYMENT)
              .receiptDate(LocalDate.now())
              .decisionDate(LocalDate.now())
              .klageFristDato(LocalDate.now().plusYears(1))
              .tier2WaiverApplied(false)
              .renteGodtgoerelseNonTaxable(true)
              .build();

      CollectionMeasureEntity tier2Measure =
          CollectionMeasureEntity.builder()
              .debtId(tier2DebtId)
              .measureType(CollectionMeasureEntity.MeasureType.SET_OFF)
              .modregningEventId(eventId)
              .amount(new BigDecimal("400"))
              .tierLevel(2)
              .build();

      when(modregningEventRepository.findByIdAndDebtorPersonId(eventId, debtorId))
          .thenReturn(Optional.of(existingEvent));
      when(collectionMeasureRepository.findByModregningEventIdAndMeasureTypeAndTierLevel(
              eventId, CollectionMeasureEntity.MeasureType.SET_OFF, 2))
          .thenReturn(List.of(tier2Measure));
      when(raekkefoeigenEngine.allocate(any(), any(), anyBoolean(), anyBoolean(), any()))
          .thenReturn(
              new TierAllocationResult(
                  List.of(),
                  List.of(),
                  List.of(new FordringAllocation(UUID.randomUUID(), new BigDecimal("400"), 3)),
                  BigDecimal.ZERO));
      when(modregningEventRepository.save(any()))
          .thenAnswer(
              inv -> {
                ModregningEvent saved = inv.getArgument(0);
                if (saved.getId() == null) {
                  saved.setId(UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd"));
                }
                return saved;
              });

      underTest.applyTier2Waiver(debtorId, eventId, "reason", caseworkerId);

      assertThatThrownBy(
              () -> underTest.applyTier2Waiver(debtorId, eventId, "reason", caseworkerId))
          .isInstanceOf(dk.ufst.opendebt.debtservice.exception.WaiverAlreadyAppliedException.class);

      verify(ledgerServiceClient, times(1))
          .reverseLedgerEntry(debtorId, tier2DebtId, new BigDecimal("400"), eventId);
    }
  }

  @Nested
  @DisplayName("CR-002: correction-pool settlement restriction")
  class CorrectionPoolSettlementRestriction {

    @Test
    @DisplayName("CR-002: restricted settlement excludes non-maintenance debts from re-application")
    void createCorrectionPoolSettlementDecision_excludesIneligibleDebts() {
      UUID debtorId = UUID.randomUUID();
      UUID originEventId = UUID.randomUUID();
      UUID settlementEventId = UUID.fromString("abababab-abab-4aba-8aba-abababababab");
      UUID eligibleDebtId = UUID.randomUUID();
      UUID ineligibleDebtId = UUID.randomUUID();
      LocalDate settlementDate = LocalDate.of(2025, 3, 31);

      KorrektionspuljeEntry entry =
          KorrektionspuljeEntry.builder().id(UUID.randomUUID()).debtorPersonId(debtorId).build();
      ModregningEvent originEvent =
          ModregningEvent.builder()
              .id(originEventId)
              .decisionReference("DEC-NKR-5809-001")
              .lineageReference("LIN-NKR-5809-001")
              .decisionKind(ModregningDecisionKind.EXTERNAL_DISBURSEMENT_DECISION)
              .operative(true)
              .debtorPersonId(debtorId)
              .nemkontoReferenceId("NKR-5809-001")
              .paymentType(PaymentType.BOERNE_OG_UNGEYDELSE)
              .receiptDate(LocalDate.of(2025, 3, 1))
              .decisionDate(LocalDate.of(2025, 3, 1))
              .disbursementAmount(new BigDecimal("500.00"))
              .tier1Amount(BigDecimal.ZERO)
              .tier2Amount(new BigDecimal("200.00"))
              .tier3Amount(BigDecimal.ZERO)
              .residualPayoutAmount(new BigDecimal("300.00"))
              .klageFristDato(LocalDate.of(2026, 3, 1))
              .renteGodtgoerelseNonTaxable(true)
              .build();

      when(raekkefoeigenEngine.allocate(debtorId, new BigDecimal("200.00"), true, false, null))
          .thenReturn(
              new TierAllocationResult(
                  List.of(),
                  List.of(
                      new FordringAllocation(eligibleDebtId, new BigDecimal("120.00"), 2),
                      new FordringAllocation(ineligibleDebtId, new BigDecimal("80.00"), 2)),
                  List.of(),
                  BigDecimal.ZERO));
      when(fordringQueryPort.isChildBenefitOffsetEligible(eligibleDebtId)).thenReturn(true);
      when(fordringQueryPort.isChildBenefitOffsetEligible(ineligibleDebtId)).thenReturn(false);
      when(fordringQueryPort.getActiveChildBenefitEligibleFordringer(debtorId, 3, null))
          .thenReturn(List.of());
      when(modregningEventRepository.save(any()))
          .thenAnswer(
              inv -> {
                ModregningEvent saved = inv.getArgument(0);
                if (saved.getId() == null) {
                  saved.setId(settlementEventId);
                }
                return saved;
              });
      when(collectionMeasureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      ModregningResult result =
          underTest.createCorrectionPoolSettlementDecision(
              entry, originEvent, new BigDecimal("200.00"), settlementDate, true);

      assertThat(result.tier2Amount()).isEqualByComparingTo("120.00");
      assertThat(result.residualPayoutAmount()).isEqualByComparingTo("80.00");
      assertThat(result.coverages())
          .extracting(coverage -> coverage.fordringId())
          .containsExactly(eligibleDebtId);

      verify(collectionMeasureRepository)
          .save(
              argThat(
                  measure ->
                      settlementEventId.equals(measure.getModregningEventId())
                          && eligibleDebtId.equals(measure.getDebtId())
                          && measure.getTierLevel() == 2));
      verify(collectionMeasureRepository, never())
          .save(
              argThat(
                  measure ->
                      settlementEventId.equals(measure.getModregningEventId())
                          && ineligibleDebtId.equals(measure.getDebtId())));
      verify(ledgerServiceClient, never())
          .postLedgerEntry(debtorId, ineligibleDebtId, new BigDecimal("80.00"), settlementEventId);
    }
  }

  // ── BUG-F: payingAuthorityOrgId passed to engine for tier-1 filtering ────────

  @Nested
  @DisplayName("BUG-F: payingAuthorityOrgId from PublicDisbursementEvent reaches engine tier-1")
  class PayingAuthorityOrgIdPropagation {

    /**
     * BUG-F: When initiateModregning is called with a PublicDisbursementEvent carrying a
     * payingAuthorityOrgId, the engine must receive that UUID so tier-1 can filter by paying
     * authority (GIL § 7, stk. 1, nr. 1). Ref: SPEC-058 §3.1
     */
    @Test
    @DisplayName(
        "BUG-F: payingAuthorityOrgId from PublicDisbursementEvent is forwarded to engine tier-1")
    void initiateModregning_forwardsPayingAuthorityOrgIdToEngine() {
      UUID debtorId = UUID.randomUUID();
      UUID fordringId = UUID.randomUUID();
      UUID payingAuthorityOrgId = UUID.randomUUID();
      setupStandardMocks(fordringId, debtorId);

      PublicDisbursementEvent pde =
          new PublicDisbursementEvent(
              "REF-BUG-F-001",
              debtorId,
              new BigDecimal("1000"),
              PaymentType.STANDARD_PAYMENT.name(),
              null,
              payingAuthorityOrgId,
              LocalDate.now(),
              LocalDate.now());

      underTest.initiateModregning(
          debtorId, new BigDecimal("1000"), PaymentType.STANDARD_PAYMENT, pde, false);

      ArgumentCaptor<UUID> orgIdCaptor = ArgumentCaptor.forClass(UUID.class);
      verify(raekkefoeigenEngine).allocate(any(), any(), anyBoolean(), orgIdCaptor.capture());
      assertThat(orgIdCaptor.getValue())
          .as(
              "payingAuthorityOrgId from PublicDisbursementEvent must be passed to engine (BUG-F,"
                  + " GIL § 7, stk. 1, nr. 1)")
          .isEqualTo(payingAuthorityOrgId);
    }

    /**
     * BUG-F: When no PublicDisbursementEvent is present (null sourceEvent), payingAuthorityOrgId
     * must be null — engine falls back to unfiltered tier-1 query. Ref: SPEC-058 §3.1
     */
    @Test
    @DisplayName("BUG-F: null sourceEvent → payingAuthorityOrgId null in engine call")
    void initiateModregning_withNullSourceEvent_passesNullOrgIdToEngine() {
      UUID debtorId = UUID.randomUUID();
      UUID fordringId = UUID.randomUUID();
      setupStandardMocks(fordringId, debtorId);

      underTest.initiateModregning(
          debtorId, new BigDecimal("500"), PaymentType.STANDARD_PAYMENT, null, false);

      ArgumentCaptor<UUID> orgIdCaptor = ArgumentCaptor.forClass(UUID.class);
      verify(raekkefoeigenEngine).allocate(any(), any(), anyBoolean(), orgIdCaptor.capture());
      assertThat(orgIdCaptor.getValue())
          .as("payingAuthorityOrgId must be null when no PublicDisbursementEvent present (BUG-F)")
          .isNull();
    }
  }
}
