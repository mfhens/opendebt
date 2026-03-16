package dk.ufst.opendebt.debtservice.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.FordringLifecycleState;
import dk.ufst.opendebt.debtservice.entity.HoeringEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringStatus;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.HoeringRepository;
import dk.ufst.opendebt.debtservice.service.HoeringService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HoeringServiceImpl implements HoeringService {

  private static final int SLA_DEADLINE_DAYS = 14;

  private final HoeringRepository hoeringRepository;
  private final DebtRepository debtRepository;

  @Override
  public HoeringEntity createHoering(UUID debtId, String deviationDescription) {
    DebtEntity debt = findDebt(debtId);

    if (debt.getLifecycleState() != FordringLifecycleState.REGISTERED) {
      throw new OpenDebtException(
          "Debt must be in REGISTERED state to create a hearing, current state: "
              + debt.getLifecycleState(),
          "HOERING_INVALID_STATE",
          OpenDebtException.ErrorSeverity.WARNING);
    }

    debt.setLifecycleState(FordringLifecycleState.HOERING);
    debtRepository.save(debt);

    HoeringEntity hoering =
        HoeringEntity.builder()
            .debtId(debtId)
            .hoeringStatus(HoeringStatus.AFVENTER_FORDRINGSHAVER)
            .deviationDescription(deviationDescription)
            .slaDeadline(LocalDateTime.now().plusDays(SLA_DEADLINE_DAYS))
            .build();

    HoeringEntity saved = hoeringRepository.save(hoering);
    log.info(
        "Created hearing {} for debt {}, SLA deadline: {}",
        saved.getId(),
        debtId,
        saved.getSlaDeadline());
    return saved;
  }

  @Override
  public HoeringEntity fordingshaverApprove(UUID hoeringId, String begrundelse) {
    HoeringEntity hoering = findHoering(hoeringId);

    validateStatus(hoering, HoeringStatus.AFVENTER_FORDRINGSHAVER, "approve");

    hoering.setFordingshaverBegrundelse(begrundelse);
    hoering.setHoeringStatus(HoeringStatus.AFVENTER_RIM);

    HoeringEntity saved = hoeringRepository.save(hoering);
    log.info("Hearing {} approved by fordringshaver, now awaiting RIM decision", saved.getId());
    return saved;
  }

  @Override
  public HoeringEntity fordingshaverWithdraw(UUID hoeringId) {
    HoeringEntity hoering = findHoering(hoeringId);

    validateStatus(hoering, HoeringStatus.AFVENTER_FORDRINGSHAVER, "withdraw");

    hoering.setHoeringStatus(HoeringStatus.FORTRUDT);
    hoering.setResolvedAt(LocalDateTime.now());

    DebtEntity debt = findDebt(hoering.getDebtId());
    debt.setLifecycleState(FordringLifecycleState.REGISTERED);
    debtRepository.save(debt);

    HoeringEntity saved = hoeringRepository.save(hoering);
    log.info(
        "Hearing {} withdrawn by fordringshaver, debt {} reset to REGISTERED",
        saved.getId(),
        hoering.getDebtId());
    return saved;
  }

  @Override
  public HoeringEntity rimDecide(UUID hoeringId, boolean accepted, String notes) {
    HoeringEntity hoering = findHoering(hoeringId);

    validateStatus(hoering, HoeringStatus.AFVENTER_RIM, "decide");

    hoering.setRimDecision(notes);
    hoering.setResolvedAt(LocalDateTime.now());

    DebtEntity debt = findDebt(hoering.getDebtId());

    if (accepted) {
      hoering.setHoeringStatus(HoeringStatus.GODKENDT);
      debt.setLifecycleState(FordringLifecycleState.OVERDRAGET);
      log.info(
          "Hearing {} accepted by RIM, debt {} transitioned to OVERDRAGET",
          hoeringId,
          hoering.getDebtId());
    } else {
      hoering.setHoeringStatus(HoeringStatus.AFVIST);
      debt.setLifecycleState(FordringLifecycleState.REGISTERED);
      log.info(
          "Hearing {} rejected by RIM, debt {} reset to REGISTERED",
          hoeringId,
          hoering.getDebtId());
    }

    debtRepository.save(debt);
    return hoeringRepository.save(hoering);
  }

  private HoeringEntity findHoering(UUID hoeringId) {
    return hoeringRepository
        .findById(hoeringId)
        .orElseThrow(
            () ->
                new OpenDebtException(
                    "Hearing not found: " + hoeringId,
                    "HOERING_NOT_FOUND",
                    OpenDebtException.ErrorSeverity.WARNING));
  }

  private DebtEntity findDebt(UUID debtId) {
    return debtRepository
        .findById(debtId)
        .orElseThrow(
            () ->
                new OpenDebtException(
                    "Debt not found: " + debtId,
                    "DEBT_NOT_FOUND",
                    OpenDebtException.ErrorSeverity.WARNING));
  }

  private void validateStatus(HoeringEntity hoering, HoeringStatus expectedStatus, String action) {
    if (hoering.getHoeringStatus() != expectedStatus) {
      throw new OpenDebtException(
          "Cannot "
              + action
              + " hearing in status "
              + hoering.getHoeringStatus()
              + ", expected "
              + expectedStatus,
          "HOERING_INVALID_STATUS",
          OpenDebtException.ErrorSeverity.WARNING);
    }
  }
}
