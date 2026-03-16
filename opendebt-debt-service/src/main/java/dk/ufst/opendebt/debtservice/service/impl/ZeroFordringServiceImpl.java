package dk.ufst.opendebt.debtservice.service.impl;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.FordringKategori;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.ZeroFordringService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements validation logic for the 0-fordring pattern (W7-ZERO-01).
 *
 * <p>A 0-fordring is a hovedfordring (HF) with principalAmount == 0 that serves as a reference
 * anchor for underfordringer (UF) such as renter after the original hovedfordring has been fully
 * paid (indfriet).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZeroFordringServiceImpl implements ZeroFordringService {

  private final DebtRepository debtRepository;

  @Override
  public void validateZeroFordring(DebtEntity entity) {
    if (entity.getPrincipalAmount() == null
        || entity.getPrincipalAmount().compareTo(BigDecimal.ZERO) != 0) {
      // Not a zero-principal claim — nothing to validate here
      return;
    }

    if (entity.getFordringKategori() != FordringKategori.HF) {
      throw new OpenDebtException(
          "Underfordring cannot have zero principal amount",
          "ZERO_PRINCIPAL_NOT_ALLOWED_FOR_UF",
          OpenDebtException.ErrorSeverity.ERROR);
    }

    // HF with principal == 0: verify all required stamdata is present
    validateRequiredStamdata(entity);

    log.info(
        "0-fordring created for rente reference: id={}, debtTypeCode={}, creditorOrgId={}",
        entity.getId(),
        entity.getDebtTypeCode(),
        entity.getCreditorOrgId());
  }

  @Override
  public void validateUnderfordringReference(DebtEntity underfordring) {
    if (underfordring.getFordringKategori() != FordringKategori.UF) {
      // Not an underfordring — no reference validation needed
      return;
    }

    UUID hovedfordringsId = underfordring.getHovedfordringsId();
    if (hovedfordringsId == null) {
      throw new OpenDebtException(
          "Underfordring must reference a hovedfordring via hovedfordringsId",
          "HOVEDFORDRINGS_ID_REQUIRED",
          OpenDebtException.ErrorSeverity.ERROR);
    }

    DebtEntity hovedfordring =
        debtRepository
            .findById(hovedfordringsId)
            .orElseThrow(
                () ->
                    new OpenDebtException(
                        "Referenced hovedfordring not found: " + hovedfordringsId,
                        "HOVEDFORDRING_NOT_FOUND",
                        OpenDebtException.ErrorSeverity.ERROR));

    if (hovedfordring.getPrincipalAmount() != null
        && hovedfordring.getPrincipalAmount().compareTo(BigDecimal.ZERO) == 0) {
      log.info(
          "Underfordring {} references 0-fordring {}", underfordring.getId(), hovedfordringsId);
    }
  }

  private void validateRequiredStamdata(DebtEntity entity) {
    if (entity.getFordringsart() == null) {
      throw new OpenDebtException(
          "0-fordring requires fordringsart", "ZERO_FORDRING_MISSING_STAMDATA");
    }
    if (entity.getDebtTypeCode() == null || entity.getDebtTypeCode().isBlank()) {
      throw new OpenDebtException(
          "0-fordring requires debtTypeCode", "ZERO_FORDRING_MISSING_STAMDATA");
    }
    if (entity.getCreditorOrgId() == null) {
      throw new OpenDebtException(
          "0-fordring requires creditorOrgId", "ZERO_FORDRING_MISSING_STAMDATA");
    }
    if (entity.getDebtorPersonId() == null) {
      throw new OpenDebtException(
          "0-fordring requires debtorPersonId", "ZERO_FORDRING_MISSING_STAMDATA");
    }
    if (entity.getForaeldelsesdato() == null) {
      throw new OpenDebtException(
          "0-fordring requires foraeldelsesdato", "ZERO_FORDRING_MISSING_STAMDATA");
    }
  }
}
