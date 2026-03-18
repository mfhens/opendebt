package dk.ufst.opendebt.debtservice.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.dto.ClaimValidationResult;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.ClaimValidationService;
import dk.ufst.opendebt.debtservice.service.ReadinessValidationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// AIDEV-NOTE: Stub implementation — real indrivelsesparathed logic must go through the Drools
// rules engine (ADR-0015). The checks here are a structural minimum only.
// AIDEV-NOTE: Injecting DebtServiceImpl directly (concrete class) because DebtService interface
// does not expose getDebtById in a way usable from within the same module without circular deps.
// AIDEV-TODO: Refactor to avoid depending on the concrete impl class; expose via interface or
// break dependency using an application event.
@Slf4j
@Service
@RequiredArgsConstructor
public class ReadinessValidationServiceImpl implements ReadinessValidationService {

  private final DebtRepository debtRepository;
  private final DebtServiceImpl debtService;
  private final ClaimValidationService claimValidationService;
  private final ObjectProvider<ReadinessValidationService> readinessValidationServiceProvider;

  @Override
  @Transactional
  public DebtDto validateReadiness(UUID debtId) {
    DebtEntity entity = findEntityById(debtId);
    DebtDto dto = debtService.getDebtById(debtId);
    ClaimValidationResult validationResult = claimValidationService.validate(dto);
    boolean ready = validationResult.isValid();
    if (ready) {
      entity.setReadinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION);
    } else {
      entity.setReadinessStatus(DebtEntity.ReadinessStatus.NOT_READY);
      String reasons =
          validationResult.getErrors().stream()
              .map(e -> e.getErrorCode() + ": " + e.getDescription())
              .reduce((a, b) -> a + "; " + b)
              .orElse("Validation failed");
      entity.setReadinessRejectionReason(reasons);
    }
    entity.setReadinessValidatedAt(LocalDateTime.now());
    debtRepository.save(entity);
    log.info("Validated readiness for debt {}: {}", debtId, entity.getReadinessStatus());
    return debtService.getDebtById(debtId);
  }

  @Override
  @Transactional
  public DebtDto approveReadiness(UUID debtId) {
    DebtEntity entity = findEntityById(debtId);
    entity.setReadinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION);
    entity.setReadinessValidatedAt(LocalDateTime.now());
    debtRepository.save(entity);
    log.info("Approved readiness for debt {}", debtId);
    return debtService.getDebtById(debtId);
  }

  @Override
  @Transactional
  public DebtDto rejectReadiness(UUID debtId, String reason) {
    DebtEntity entity = findEntityById(debtId);
    entity.setReadinessStatus(DebtEntity.ReadinessStatus.NOT_READY);
    entity.setReadinessRejectionReason(reason);
    entity.setReadinessValidatedAt(LocalDateTime.now());
    debtRepository.save(entity);
    log.info("Rejected readiness for debt {}, reason: {}", debtId, reason);
    return debtService.getDebtById(debtId);
  }

  @Override
  public int validateBatchReadiness(String creditorId) {
    UUID creditorOrgId = UUID.fromString(creditorId);
    var debts =
        debtRepository.findByCreditorOrgIdAndReadinessStatus(
            creditorOrgId, DebtEntity.ReadinessStatus.PENDING_REVIEW);
    ReadinessValidationService readinessValidationService =
        readinessValidationServiceProvider.getObject();
    // AIDEV-NOTE: Sequential iteration — acceptable for now; consider parallel stream or async
    // processing once batch sizes from real fordringshavere are known.
    debts.forEach(d -> readinessValidationService.validateReadiness(d.getId()));
    log.info("Validated batch readiness for creditor {}: {} debts", creditorId, debts.size());
    return debts.size();
  }

  private DebtEntity findEntityById(UUID id) {
    return debtRepository
        .findById(id)
        .orElseThrow(
            () ->
                new OpenDebtException(
                    "Debt not found: " + id,
                    "DEBT_NOT_FOUND",
                    OpenDebtException.ErrorSeverity.WARNING));
  }
}
