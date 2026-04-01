package dk.ufst.opendebt.debtservice.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.common.audit.cls.ClsAuditEvent;
import dk.ufst.opendebt.debtservice.dto.ClaimAdjustmentRequestDto;
import dk.ufst.opendebt.debtservice.dto.ClaimAdjustmentResponseDto;
import dk.ufst.opendebt.debtservice.entity.ClaimCategory;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringStatus;
import dk.ufst.opendebt.debtservice.exception.CreditorValidationException;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.HoeringRepository;
import dk.ufst.opendebt.debtservice.service.ClaimAdjustmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link ClaimAdjustmentService}. Enforces all FR-9 legal constraints
 * independently of portal-side validation (SPEC-P053 §9.3).
 *
 * <p>Validation rules (SPEC-P053 §9.3):
 *
 * <ol>
 *   <li>FR-1: WriteDownReasonCode required when direction == WRITE_DOWN
 *   <li>FR-2: OPSKRIVNING_REGULERING on RENTE claim → 422
 *   <li>FR-3: Høring timing rule applied server-side
 *   <li>FR-4: Retroactive log marker when virkningsdato &lt; today
 *   <li>FR-6: GIL § 18 k cross-system flag
 *   <li>FR-7: DINDB/OMPL/AFSK denylist → 422
 * </ol>
 *
 * <p>Audit: all outcomes (success and failure) are logged to CLS (AC-16 / SPEC-P053 §9.4).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimAdjustmentServiceImpl implements ClaimAdjustmentService {

  /** RIM-internal write-up reason codes that must not be submitted by fordringshavere (FR-7). */
  private static final Set<String> RIM_INTERNAL_CODES = Set.of("DINDB", "OMPL", "AFSK");

  /** All legal adjustment type values (SPEC-P053 §9.3 / B4 validation). */
  private static final Set<String> LEGAL_ADJUSTMENT_TYPES =
      Set.of(
          "NEDSKRIV",
          "NEDSKRIVNING",
          "NEDSKRIVNING_INDBETALING",
          "NEDSKRIVNING_ANNULLERET_OPSKRIVNING_REGULERING",
          "NEDSKRIVNING_ANNULLERET_OPSKRIVNING_INDBETALING",
          "WRITE_DOWN",
          "OPSKRIVNING_REGULERING",
          "OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING",
          "OPSKRIVNING_ANNULLERET_NEDSKRIVNING_INDBETALING",
          "FEJLAGTIG_HOVEDSTOL_INDBERETNING",
          "WRITE_UP");

  /** Prefix used by write-down adjustment types (for direction detection). */
  private static final String WRITE_DOWN_TYPE_PREFIX = "NEDSKRIVNING";

  /** Suffix for NEDSKRIV adjustment type (simple write-down). */
  private static final String WRITE_DOWN_SIMPLE = "NEDSKRIV";

  /** Type name for the opskrivning regulering path that is disallowed for RENTE claims (FR-2). */
  private static final String OPSKRIVNING_REGULERING = "OPSKRIVNING_REGULERING";

  private final DebtRepository debtRepository;
  private final HoeringRepository hoeringRepository;
  private final ClsAuditClient clsAuditClient;

  @Override
  @Transactional
  public ClaimAdjustmentResponseDto processAdjustment(
      UUID claimId, ClaimAdjustmentRequestDto request) {

    String adjustmentType = request.getAdjustmentType();

    // --- B4: adjustmentType whitelist validation ---
    if (adjustmentType == null || !LEGAL_ADJUSTMENT_TYPES.contains(adjustmentType)) {
      // claimId known but claim not yet loaded — audit with null for claimId-level detail
      shipAuditFailure(claimId, adjustmentType, "INVALID_ADJUSTMENT_TYPE");
      throw new CreditorValidationException(
          "Unknown adjustmentType: '"
              + adjustmentType
              + "'. Must be one of: "
              + LEGAL_ADJUSTMENT_TYPES,
          "INVALID_ADJUSTMENT_TYPE");
    }

    // Load claim — 404 if not found (B2 + B3)
    DebtEntity debt =
        debtRepository
            .findById(claimId)
            .orElseThrow(
                () -> {
                  // B3: CLS audit before throwing not-found
                  shipAuditFailure(claimId, adjustmentType, "CLAIM_NOT_FOUND");
                  // B2: HTTP 404, not 422
                  return new ResponseStatusException(
                      HttpStatus.NOT_FOUND, "Claim not found: " + claimId);
                });

    boolean isWriteDown = isWriteDownDirection(adjustmentType);

    // --- FR-1: WriteDownReasonCode required for write-down ---
    if (isWriteDown) {
      if (request.getWriteDownReasonCode() == null) {
        shipAuditFailure(claimId, adjustmentType, null);
        throw new CreditorValidationException(
            "WriteDownReasonCode is required and must be one of the legal values"
                + " (NED_INDBETALING, NED_FEJL_OVERSENDELSE, NED_GRUNDLAG_AENDRET)"
                + " per Gæld.bekendtg. § 7 stk. 2.",
            "WRITE_DOWN_REASON_REQUIRED");
      }
    }

    // --- FR-7: RIM-internal codes denylist ---
    if (request.getWriteUpReasonCode() != null
        && RIM_INTERNAL_CODES.contains(request.getWriteUpReasonCode())) {
      shipAuditFailure(claimId, adjustmentType, null);
      throw new CreditorValidationException(
          "Reason code is reserved for RIM-internal operations (G.A.2.3.4.4): "
              + request.getWriteUpReasonCode(),
          "RIM_INTERNAL_CODE");
    }

    // --- FR-2: OPSKRIVNING_REGULERING on RENTE claim (M4: null-safe enum compare) ---
    if (OPSKRIVNING_REGULERING.equals(adjustmentType)
        && ClaimCategory.RENTE.equals(debt.getClaimCategory())) {
      shipAuditFailure(claimId, adjustmentType, null);
      throw new CreditorValidationException(
          "RENTE claims must use a rentefordring, not an opskrivningsfordring (G.A.1.4.3).",
          "RENTE_OPSKRIVNING_FORBIDDEN");
    }

    // --- FR-3: Høring timing rule (B1) ---
    // For ALL adjustment types on a HOERING claim, the response status is PENDING_HOERING to
    // indicate the opskrivningsfordring receipt time is the høring resolution time.
    // For OPSKRIVNING_REGULERING specifically, the actual høring resolution timestamp is fetched
    // and used as the authoritative receipt time. (G.A.1.4.3, Gæld.bekendtg. § 7 stk. 1, 4. pkt.)
    ReceiptState receiptState = computeReceiptState(claimId, adjustmentType, debt);
    Instant receiptTimestamp = receiptState.timestamp();
    String status = receiptState.status();

    // --- FR-4: Retroactive nedskrivning log marker ---
    if (isWriteDown
        && request.getEffectiveDate() != null
        && request.getEffectiveDate().isBefore(LocalDate.now())) {
      log.warn(
          "RETROACTIVE_NEDSKRIVNING claimId={} virkningsdato={}",
          claimId,
          request.getEffectiveDate());
    }

    // --- FR-6: GIL § 18 k cross-system flag evaluation (SPEC-P053 §6.2) ---
    boolean crossSystemRetroactiveApplies =
        isWriteDown
            && request.getEffectiveDate() != null
            && debt.getReceivedAt() != null
            && request.getEffectiveDate().isBefore(debt.getReceivedAt().toLocalDate());

    // --- AC-16: CLS audit — success path ---
    shipAuditSuccess(claimId, adjustmentType, request);

    BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;

    return ClaimAdjustmentResponseDto.builder()
        .actionId("ACT-" + claimId.toString().substring(0, 8).toUpperCase())
        .status(status)
        .amount(amount)
        .crossSystemRetroactiveApplies(crossSystemRetroactiveApplies)
        .receiptTimestamp(receiptTimestamp)
        .build();
  }

  private record ReceiptState(Instant timestamp, String status) {}

  /**
   * Determines the receipt timestamp and status string for the FR-3 høring timing rule. Returns
   * PENDING_HOERING for any adjustment on a HOERING claim, using the actual høring resolution time
   * when the type is OPSKRIVNING_REGULERING.
   */
  private ReceiptState computeReceiptState(UUID claimId, String adjustmentType, DebtEntity debt) {
    if (ClaimLifecycleState.HOERING != debt.getLifecycleState()) {
      return new ReceiptState(Instant.now(), "ACCEPTED");
    }
    if (OPSKRIVNING_REGULERING.equals(adjustmentType)) {
      return new ReceiptState(
          fetchHoeringReceiptTimestamp(claimId, adjustmentType), "PENDING_HOERING");
    }
    return new ReceiptState(Instant.now(), "PENDING_HOERING");
  }

  /**
   * Fetches the authoritative receipt timestamp from the resolved høring record for
   * OPSKRIVNING_REGULERING adjustments (Gæld.bekendtg. § 7 stk. 1, 4. pkt.).
   */
  private Instant fetchHoeringReceiptTimestamp(UUID claimId, String adjustmentType) {
    HoeringEntity hoering =
        hoeringRepository
            .findTopByDebtIdOrderByResolvedAtDesc(claimId)
            .orElseThrow(
                () -> {
                  shipAuditFailure(claimId, adjustmentType, "HOERING_RECORD_MISSING");
                  return new CreditorValidationException(
                      "Claim is in HOERING state but no høring record exists for claim: " + claimId,
                      "HOERING_RECORD_MISSING");
                });
    LocalDateTime resolvedAt = hoering.getResolvedAt();
    boolean statusIsResolved =
        hoering.getHoeringStatus() != HoeringStatus.AFVENTER_FORDRINGSHAVER
            && hoering.getHoeringStatus() != HoeringStatus.AFVENTER_RIM;
    if (resolvedAt == null || !statusIsResolved) {
      shipAuditFailure(claimId, adjustmentType, "HOERING_NOT_YET_RESOLVED");
      throw new CreditorValidationException(
          "Cannot process OPSKRIVNING_REGULERING: høring for claim "
              + claimId
              + " has not yet been resolved (Gæld.bekendtg. § 7 stk. 1, 4. pkt.).",
          "HOERING_NOT_YET_RESOLVED");
    }
    Instant receiptTimestamp = resolvedAt.atZone(java.time.ZoneOffset.UTC).toInstant();
    log.info(
        "HOERING_TIMING_RULE claimId={} adjustmentType={}: receipt timestamp set to"
            + " høring resolution time {} (not portal submission time) (G.A.1.4.3,"
            + " Gæld.bekendtg. § 7 stk. 1, 4. pkt.)",
        claimId,
        adjustmentType,
        receiptTimestamp);
    return receiptTimestamp;
  }

  /**
   * Returns true when the adjustment type represents a write-down (nedskrivning) direction.
   * Write-down types start with "NEDSKRIVNING" or are the simple "NEDSKRIV" type.
   */
  private boolean isWriteDownDirection(String adjustmentType) {
    if (adjustmentType == null) {
      return false;
    }
    return adjustmentType.startsWith(WRITE_DOWN_TYPE_PREFIX)
        || WRITE_DOWN_SIMPLE.equals(adjustmentType)
        || "WRITE_DOWN".equals(adjustmentType);
  }

  /** Ships a CLS audit event for a successful adjustment (AC-16 / SPEC-P053 §9.4). */
  private void shipAuditSuccess(
      UUID claimId, String adjustmentType, ClaimAdjustmentRequestDto request) {
    try {
      ClsAuditEvent event =
          ClsAuditEvent.builder()
              .eventId(UUID.randomUUID())
              .timestamp(Instant.now())
              .serviceName("debt-service")
              .operation("ADJUSTMENT_SUCCESS")
              .resourceType("claim")
              .resourceId(claimId)
              .newValues(
                  java.util.Map.of(
                      "adjustmentType", adjustmentType != null ? adjustmentType : "",
                      "reasonCode",
                          request.getWriteDownReasonCode() != null
                              ? request.getWriteDownReasonCode().name()
                              : "",
                      "outcome", "SUCCESS"))
              .build();
      clsAuditClient.shipEvent(event);
    } catch (Exception ex) {
      log.warn(
          "CLS audit shipment failed for claim {} (adjustment success): {}",
          claimId,
          ex.getMessage());
    }
  }

  /** Ships a CLS audit event for a failed adjustment (AC-16 / SPEC-P053 §9.4). */
  private void shipAuditFailure(UUID claimId, String adjustmentType, String reasonCode) {
    try {
      ClsAuditEvent event =
          ClsAuditEvent.builder()
              .eventId(UUID.randomUUID())
              .timestamp(Instant.now())
              .serviceName("debt-service")
              .operation("ADJUSTMENT_FAILURE")
              .resourceType("claim")
              .resourceId(claimId)
              .newValues(
                  java.util.Map.of(
                      "adjustmentType", adjustmentType != null ? adjustmentType : "",
                      "reasonCode", reasonCode != null ? reasonCode : "",
                      "outcome", "FAILURE"))
              .build();
      clsAuditClient.shipEvent(event);
    } catch (Exception ex) {
      log.warn(
          "CLS audit shipment failed for claim {} (adjustment failure): {}",
          claimId,
          ex.getMessage());
    }
  }
}
