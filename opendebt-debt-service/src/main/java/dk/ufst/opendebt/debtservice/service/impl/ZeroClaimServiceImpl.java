package dk.ufst.opendebt.debtservice.service.impl;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.entity.ClaimCategory;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.ZeroClaimService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements validation logic for the zero-principal claim pattern (W7-ZERO-01).
 *
 * <p>A zero-principal claim is a main claim (HF) with principalAmount == 0 that serves as a
 * reference anchor for sub-claims (UF) such as interest claims after the original main claim has
 * been fully paid.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZeroClaimServiceImpl implements ZeroClaimService {

  private final DebtRepository debtRepository;

  @Override
  public void validateZeroClaim(DebtEntity entity) {
    if (entity.getPrincipalAmount() == null
        || entity.getPrincipalAmount().compareTo(BigDecimal.ZERO) != 0) {
      // Not a zero-principal claim — nothing to validate here
      return;
    }

    if (entity.getClaimCategory() != ClaimCategory.HF) {
      throw new OpenDebtException(
          "Sub-claim cannot have zero principal amount",
          "ZERO_PRINCIPAL_NOT_ALLOWED_FOR_UF",
          OpenDebtException.ErrorSeverity.ERROR);
    }

    // HF with principal == 0: verify all required master data is present
    validateRequiredStamdata(entity);

    log.info(
        "Zero-principal claim created for interest reference: id={}, debtTypeCode={}, creditorOrgId={}",
        entity.getId(),
        entity.getDebtTypeCode(),
        entity.getCreditorOrgId());
  }

  @Override
  public void validateSubClaimReference(DebtEntity subClaim) {
    if (subClaim.getClaimCategory() != ClaimCategory.UF) {
      // Not a sub-claim — no reference validation needed
      return;
    }

    UUID parentClaimId = subClaim.getParentClaimId();
    if (parentClaimId == null) {
      throw new OpenDebtException(
          "Sub-claim must reference a main claim via parentClaimId",
          "PARENT_CLAIM_ID_REQUIRED",
          OpenDebtException.ErrorSeverity.ERROR);
    }

    DebtEntity parentClaim =
        debtRepository
            .findById(parentClaimId)
            .orElseThrow(
                () ->
                    new OpenDebtException(
                        "Referenced main claim not found: " + parentClaimId,
                        "PARENT_CLAIM_NOT_FOUND",
                        OpenDebtException.ErrorSeverity.ERROR));

    if (parentClaim.getPrincipalAmount() != null
        && parentClaim.getPrincipalAmount().compareTo(BigDecimal.ZERO) == 0) {
      log.info("Sub-claim {} references zero-principal claim {}", subClaim.getId(), parentClaimId);
    }
  }

  private void validateRequiredStamdata(DebtEntity entity) {
    if (entity.getClaimArt() == null) {
      throw new OpenDebtException(
          "Zero-principal claim requires claimArt", "ZERO_CLAIM_MISSING_STAMDATA");
    }
    if (entity.getDebtTypeCode() == null || entity.getDebtTypeCode().isBlank()) {
      throw new OpenDebtException(
          "Zero-principal claim requires debtTypeCode", "ZERO_CLAIM_MISSING_STAMDATA");
    }
    if (entity.getCreditorOrgId() == null) {
      throw new OpenDebtException(
          "Zero-principal claim requires creditorOrgId", "ZERO_CLAIM_MISSING_STAMDATA");
    }
    if (entity.getDebtorPersonId() == null) {
      throw new OpenDebtException(
          "Zero-principal claim requires debtorPersonId", "ZERO_CLAIM_MISSING_STAMDATA");
    }
    if (entity.getLimitationDate() == null) {
      throw new OpenDebtException(
          "Zero-principal claim requires limitationDate", "ZERO_CLAIM_MISSING_STAMDATA");
    }
  }
}
