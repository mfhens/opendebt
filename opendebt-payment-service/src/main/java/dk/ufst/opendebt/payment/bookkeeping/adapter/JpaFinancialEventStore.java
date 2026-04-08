package dk.ufst.opendebt.payment.bookkeeping.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.domain.FinancialEvent;
import dk.ufst.bookkeeping.port.FinancialEventStore;
import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;
import dk.ufst.opendebt.payment.bookkeeping.repository.DebtEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JpaFinancialEventStore implements FinancialEventStore {

  private final DebtEventRepository debtEventRepository;

  @Override
  public FinancialEvent save(FinancialEvent event) {
    DebtEventEntity entity = toEntity(event);
    DebtEventEntity saved = debtEventRepository.save(entity);
    return fromEntity(saved);
  }

  @Override
  public List<FinancialEvent> findPrincipalAffectingEvents(UUID debtId) {
    return debtEventRepository.findPrincipalAffectingEvents(debtId).stream()
        .map(this::fromEntity)
        .toList();
  }

  @Override
  public List<FinancialEvent> findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(UUID debtId) {
    return debtEventRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(debtId).stream()
        .map(this::fromEntity)
        .toList();
  }

  private DebtEventEntity toEntity(FinancialEvent event) {
    return DebtEventEntity.builder()
        .id(event.getId())
        .debtId(event.getDebtId())
        .eventType(toEntityEventType(event.getEventType()))
        .effectiveDate(event.getEffectiveDate())
        .amount(event.getAmount())
        .correctsEventId(event.getCorrectsEventId())
        .reference(event.getReference())
        .description(event.getDescription())
        .ledgerTransactionId(event.getLedgerTransactionId())
        .build();
  }

  private FinancialEvent fromEntity(DebtEventEntity entity) {
    return FinancialEvent.builder()
        .id(entity.getId())
        .debtId(entity.getDebtId())
        .eventType(fromEntityEventType(entity.getEventType()))
        .effectiveDate(entity.getEffectiveDate())
        .amount(entity.getAmount())
        .correctsEventId(entity.getCorrectsEventId())
        .reference(entity.getReference())
        .description(entity.getDescription())
        .ledgerTransactionId(entity.getLedgerTransactionId())
        .createdAt(entity.getCreatedAt())
        .build();
  }

  private DebtEventEntity.EventType toEntityEventType(EventType type) {
    return switch (type) {
      case DEBT_REGISTERED -> DebtEventEntity.EventType.DEBT_REGISTERED;
      case PAYMENT_RECEIVED -> DebtEventEntity.EventType.PAYMENT_RECEIVED;
      case INTEREST_ACCRUED -> DebtEventEntity.EventType.INTEREST_ACCRUED;
      case OFFSETTING_EXECUTED -> DebtEventEntity.EventType.OFFSETTING_EXECUTED;
      case WRITE_OFF -> DebtEventEntity.EventType.WRITE_OFF;
      case REFUND -> DebtEventEntity.EventType.REFUND;
      case UDLAEG_REGISTERED -> DebtEventEntity.EventType.UDLAEG_REGISTERED;
      case UDLAEG_CORRECTED -> DebtEventEntity.EventType.UDLAEG_CORRECTED;
      case CORRECTION -> DebtEventEntity.EventType.CORRECTION;
      case COVERAGE_REVERSED -> DebtEventEntity.EventType.COVERAGE_REVERSED;
    };
  }

  private EventType fromEntityEventType(DebtEventEntity.EventType type) {
    return switch (type) {
      case DEBT_REGISTERED -> EventType.DEBT_REGISTERED;
      case PAYMENT_RECEIVED -> EventType.PAYMENT_RECEIVED;
      case INTEREST_ACCRUED -> EventType.INTEREST_ACCRUED;
      case OFFSETTING_EXECUTED -> EventType.OFFSETTING_EXECUTED;
      case WRITE_OFF -> EventType.WRITE_OFF;
      case REFUND -> EventType.REFUND;
      case UDLAEG_REGISTERED -> EventType.UDLAEG_REGISTERED;
      case UDLAEG_CORRECTED -> EventType.UDLAEG_CORRECTED;
      case CORRECTION -> EventType.CORRECTION;
      case COVERAGE_REVERSED -> EventType.COVERAGE_REVERSED;
    };
  }
}
