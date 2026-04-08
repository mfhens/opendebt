package dk.ufst.opendebt.payment.bookkeeping.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.domain.FinancialEvent;
import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;
import dk.ufst.opendebt.payment.bookkeeping.repository.DebtEventRepository;

@ExtendWith(MockitoExtension.class)
class JpaFinancialEventStoreTest {

  @Mock private DebtEventRepository debtEventRepository;

  @InjectMocks private JpaFinancialEventStore store;

  // --- save ---

  @Test
  void save_persistsEventAndReturnsMappedResult() {
    UUID id = UUID.randomUUID();
    UUID debtId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    FinancialEvent event =
        FinancialEvent.builder()
            .debtId(debtId)
            .eventType(EventType.PAYMENT_RECEIVED)
            .effectiveDate(LocalDate.of(2025, 3, 1))
            .amount(new BigDecimal("1000.00"))
            .reference("CREMUL-001")
            .description("Betaling")
            .build();

    DebtEventEntity savedEntity =
        DebtEventEntity.builder()
            .id(id)
            .debtId(debtId)
            .eventType(DebtEventEntity.EventType.PAYMENT_RECEIVED)
            .effectiveDate(LocalDate.of(2025, 3, 1))
            .amount(new BigDecimal("1000.00"))
            .reference("CREMUL-001")
            .description("Betaling")
            .createdAt(now)
            .build();

    when(debtEventRepository.save(any())).thenReturn(savedEntity);

    FinancialEvent result = store.save(event);

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getDebtId()).isEqualTo(debtId);
    assertThat(result.getEventType()).isEqualTo(EventType.PAYMENT_RECEIVED);
    assertThat(result.getCreatedAt()).isEqualTo(now);
    verify(debtEventRepository).save(any(DebtEventEntity.class));
  }

  // --- findPrincipalAffectingEvents ---

  @Test
  void findPrincipalAffectingEvents_returnsMappedEvents() {
    UUID debtId = UUID.randomUUID();
    DebtEventEntity entity = buildEntity(DebtEventEntity.EventType.DEBT_REGISTERED);
    when(debtEventRepository.findPrincipalAffectingEvents(debtId)).thenReturn(List.of(entity));

    List<FinancialEvent> result = store.findPrincipalAffectingEvents(debtId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getEventType()).isEqualTo(EventType.DEBT_REGISTERED);
  }

  // --- findByDebtIdOrderByEffectiveDateAscCreatedAtAsc ---

  @Test
  void findByDebtIdOrdered_returnsMappedEvents() {
    UUID debtId = UUID.randomUUID();
    DebtEventEntity entity = buildEntity(DebtEventEntity.EventType.INTEREST_ACCRUED);
    when(debtEventRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(debtId))
        .thenReturn(List.of(entity));

    List<FinancialEvent> result = store.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(debtId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getEventType()).isEqualTo(EventType.INTEREST_ACCRUED);
  }

  // --- enum mapping coverage ---

  @Test
  void save_mapsAllEventTypesToEntity() {
    for (EventType type : EventType.values()) {
      FinancialEvent event =
          FinancialEvent.builder()
              .debtId(UUID.randomUUID())
              .eventType(type)
              .effectiveDate(LocalDate.now())
              .amount(BigDecimal.TEN)
              .build();

      DebtEventEntity savedEntity =
          DebtEventEntity.builder()
              .id(UUID.randomUUID())
              .eventType(DebtEventEntity.EventType.valueOf(type.name()))
              .build();

      when(debtEventRepository.save(any())).thenReturn(savedEntity);
      store.save(event);
    }
  }

  @Test
  void findPrincipalAffectingEvents_mapsAllEntityEventTypesBack() {
    UUID debtId = UUID.randomUUID();
    for (DebtEventEntity.EventType type : DebtEventEntity.EventType.values()) {
      DebtEventEntity entity = buildEntity(type);
      when(debtEventRepository.findPrincipalAffectingEvents(debtId)).thenReturn(List.of(entity));
      store.findPrincipalAffectingEvents(debtId);
    }
  }

  // --- helpers ---

  private DebtEventEntity buildEntity(DebtEventEntity.EventType type) {
    return DebtEventEntity.builder()
        .id(UUID.randomUUID())
        .debtId(UUID.randomUUID())
        .eventType(type)
        .effectiveDate(LocalDate.now())
        .amount(new BigDecimal("500.00"))
        .reference("REF")
        .description("desc")
        .build();
  }
}
