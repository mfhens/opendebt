package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.debtservice.dto.CollectionMeasureDto;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity;
import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity.MeasureStatus;
import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity.MeasureType;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.CollectionMeasureRepository;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class CollectionMeasureServiceImplTest {

  @Mock private CollectionMeasureRepository measureRepository;
  @Mock private DebtRepository debtRepository;
  @Mock private NotificationService notificationService;

  private CollectionMeasureServiceImpl service;

  private static final UUID DEBT_ID = UUID.randomUUID();

  private DebtEntity overdragetDebt;

  @BeforeEach
  void setUp() {
    service =
        new CollectionMeasureServiceImpl(measureRepository, debtRepository, notificationService);
    overdragetDebt =
        DebtEntity.builder()
            .id(DEBT_ID)
            .debtorPersonId(UUID.randomUUID())
            .creditorOrgId(UUID.randomUUID())
            .principalAmount(new BigDecimal("10000"))
            .dueDate(LocalDate.now().minusMonths(3))
            .debtTypeCode("600")
            .status(DebtEntity.DebtStatus.IN_COLLECTION)
            .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
            .lifecycleState(ClaimLifecycleState.OVERDRAGET)
            .build();
  }

  @Test
  void initiateMeasure_setOff_createsSuccessfully() {
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(overdragetDebt));
    when(measureRepository.save(any()))
        .thenAnswer(
            inv -> {
              CollectionMeasureEntity e = inv.getArgument(0);
              e.setId(UUID.randomUUID());
              return e;
            });

    CollectionMeasureDto result =
        service.initiateMeasure(DEBT_ID, MeasureType.SET_OFF, new BigDecimal("5000"), null);

    assertThat(result.getMeasureType()).isEqualTo("SET_OFF");
    assertThat(result.getStatus()).isEqualTo("INITIATED");
    assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("5000"));
  }

  // Ref: G.A.3.1.4 — notifyModregning must be called when a SET_OFF measure is initiated.
  @Test
  void initiateMeasure_setOff_callsNotifyModregning() {
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(overdragetDebt));
    when(measureRepository.save(any()))
        .thenAnswer(
            inv -> {
              CollectionMeasureEntity e = inv.getArgument(0);
              e.setId(UUID.randomUUID());
              return e;
            });

    service.initiateMeasure(DEBT_ID, MeasureType.SET_OFF, new BigDecimal("3000"), null);

    verify(notificationService).notifyModregning(DEBT_ID, new BigDecimal("3000"));
  }

  // Ref: G.A.3.1.4 — notifyModregning must NOT be called for non-SET_OFF measure types.
  @Test
  void initiateMeasure_wageGarnishment_doesNotCallNotifyModregning() {
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(overdragetDebt));
    when(measureRepository.save(any()))
        .thenAnswer(
            inv -> {
              CollectionMeasureEntity e = inv.getArgument(0);
              e.setId(UUID.randomUUID());
              return e;
            });

    service.initiateMeasure(DEBT_ID, MeasureType.WAGE_GARNISHMENT, new BigDecimal("1000"), null);

    verify(notificationService, never()).notifyModregning(any(), any());
  }

  @Test
  void initiateMeasure_wageGarnishment_createsSuccessfully() {
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(overdragetDebt));
    when(measureRepository.save(any()))
        .thenAnswer(
            inv -> {
              CollectionMeasureEntity e = inv.getArgument(0);
              e.setId(UUID.randomUUID());
              return e;
            });

    CollectionMeasureDto result =
        service.initiateMeasure(DEBT_ID, MeasureType.WAGE_GARNISHMENT, null, null);

    assertThat(result.getMeasureType()).isEqualTo("WAGE_GARNISHMENT");
    assertThat(result.getStatus()).isEqualTo("INITIATED");
  }

  @Test
  void initiateMeasure_attachment_createsSuccessfully() {
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(overdragetDebt));
    when(measureRepository.save(any()))
        .thenAnswer(
            inv -> {
              CollectionMeasureEntity e = inv.getArgument(0);
              e.setId(UUID.randomUUID());
              return e;
            });

    CollectionMeasureDto result =
        service.initiateMeasure(DEBT_ID, MeasureType.ATTACHMENT, null, "property attachment");

    assertThat(result.getMeasureType()).isEqualTo("ATTACHMENT");
    assertThat(result.getNote()).isEqualTo("property attachment");
  }

  @Test
  void initiateMeasure_notOverdraget_throws() {
    DebtEntity registeredDebt =
        DebtEntity.builder()
            .id(DEBT_ID)
            .debtorPersonId(UUID.randomUUID())
            .creditorOrgId(UUID.randomUUID())
            .principalAmount(new BigDecimal("10000"))
            .dueDate(LocalDate.now())
            .debtTypeCode("600")
            .status(DebtEntity.DebtStatus.ACTIVE)
            .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
            .lifecycleState(ClaimLifecycleState.REGISTERED)
            .build();

    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(registeredDebt));

    assertThatThrownBy(() -> service.initiateMeasure(DEBT_ID, MeasureType.SET_OFF, null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("OVERDRAGET");
  }

  @Test
  void initiateMeasure_debtNotFound_throws() {
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.initiateMeasure(DEBT_ID, MeasureType.SET_OFF, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Debt not found");
  }

  @Test
  void completeMeasure_setsCompletedStatus() {
    UUID measureId = UUID.randomUUID();
    CollectionMeasureEntity entity =
        CollectionMeasureEntity.builder()
            .id(measureId)
            .debtId(DEBT_ID)
            .measureType(MeasureType.SET_OFF)
            .status(MeasureStatus.INITIATED)
            .initiatedAt(Instant.now())
            .build();

    when(measureRepository.findById(measureId)).thenReturn(Optional.of(entity));
    when(measureRepository.save(any())).thenReturn(entity);

    CollectionMeasureDto result = service.completeMeasure(measureId);

    assertThat(result.getStatus()).isEqualTo("COMPLETED");
    assertThat(entity.getCompletedAt()).isNotNull();
  }

  @Test
  void completeMeasure_alreadyCompleted_throws() {
    UUID measureId = UUID.randomUUID();
    CollectionMeasureEntity entity =
        CollectionMeasureEntity.builder()
            .id(measureId)
            .debtId(DEBT_ID)
            .measureType(MeasureType.SET_OFF)
            .status(MeasureStatus.COMPLETED)
            .build();

    when(measureRepository.findById(measureId)).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> service.completeMeasure(measureId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot complete");
  }

  @Test
  void cancelMeasure_setsCancelledStatus() {
    UUID measureId = UUID.randomUUID();
    CollectionMeasureEntity entity =
        CollectionMeasureEntity.builder()
            .id(measureId)
            .debtId(DEBT_ID)
            .measureType(MeasureType.WAGE_GARNISHMENT)
            .status(MeasureStatus.INITIATED)
            .initiatedAt(Instant.now())
            .build();

    when(measureRepository.findById(measureId)).thenReturn(Optional.of(entity));
    when(measureRepository.save(any())).thenReturn(entity);

    CollectionMeasureDto result = service.cancelMeasure(measureId, "debtor arranged payment");

    assertThat(result.getStatus()).isEqualTo("CANCELLED");
    assertThat(entity.getNote()).isEqualTo("debtor arranged payment");
  }

  @Test
  void cancelMeasure_alreadyCancelled_throws() {
    UUID measureId = UUID.randomUUID();
    CollectionMeasureEntity entity =
        CollectionMeasureEntity.builder()
            .id(measureId)
            .debtId(DEBT_ID)
            .measureType(MeasureType.SET_OFF)
            .status(MeasureStatus.CANCELLED)
            .build();

    when(measureRepository.findById(measureId)).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> service.cancelMeasure(measureId, "reason"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot cancel");
  }

  @Test
  void getMeasures_returnsList() {
    CollectionMeasureEntity entity =
        CollectionMeasureEntity.builder()
            .id(UUID.randomUUID())
            .debtId(DEBT_ID)
            .measureType(MeasureType.ATTACHMENT)
            .status(MeasureStatus.INITIATED)
            .initiatedAt(Instant.now())
            .build();

    when(measureRepository.findByDebtIdOrderByInitiatedAtDesc(DEBT_ID)).thenReturn(List.of(entity));

    List<CollectionMeasureDto> result = service.getMeasures(DEBT_ID);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getMeasureType()).isEqualTo("ATTACHMENT");
  }

  @Test
  void completeMeasure_notFound_throws() {
    UUID measureId = UUID.randomUUID();
    when(measureRepository.findById(measureId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.completeMeasure(measureId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }
}
