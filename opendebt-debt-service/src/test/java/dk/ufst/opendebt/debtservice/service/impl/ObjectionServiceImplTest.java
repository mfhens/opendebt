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

import dk.ufst.opendebt.debtservice.dto.ObjectionDto;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.ObjectionEntity;
import dk.ufst.opendebt.debtservice.entity.ObjectionEntity.ObjectionStatus;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.ObjectionRepository;

@ExtendWith(MockitoExtension.class)
class ObjectionServiceImplTest {

  @Mock private ObjectionRepository objectionRepository;
  @Mock private DebtRepository debtRepository;

  private ObjectionServiceImpl service;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final UUID DEBTOR_PERSON_ID = UUID.randomUUID();
  private DebtEntity testDebt;

  @BeforeEach
  void setUp() {
    service = new ObjectionServiceImpl(objectionRepository, debtRepository);
    testDebt =
        DebtEntity.builder()
            .id(DEBT_ID)
            .debtorPersonId(DEBTOR_PERSON_ID)
            .creditorOrgId(UUID.randomUUID())
            .principalAmount(new BigDecimal("10000"))
            .dueDate(LocalDate.now().minusMonths(1))
            .debtTypeCode("600")
            .status(DebtEntity.DebtStatus.ACTIVE)
            .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
            .build();
  }

  @Test
  void registerObjection_createsActiveObjection() {
    when(debtRepository.existsById(DEBT_ID)).thenReturn(true);
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(testDebt));
    when(objectionRepository.existsByDebtIdAndStatus(DEBT_ID, ObjectionStatus.ACTIVE))
        .thenReturn(false);
    when(objectionRepository.save(any()))
        .thenAnswer(
            inv -> {
              ObjectionEntity e = inv.getArgument(0);
              e.setId(UUID.randomUUID());
              return e;
            });
    when(debtRepository.save(any())).thenReturn(testDebt);

    ObjectionDto result =
        service.registerObjection(DEBT_ID, DEBTOR_PERSON_ID, "claim amount disputed");

    assertThat(result.getStatus()).isEqualTo("ACTIVE");
    assertThat(result.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(result.getDebtorPersonId()).isEqualTo(DEBTOR_PERSON_ID);
    assertThat(result.getReason()).isEqualTo("claim amount disputed");
    assertThat(testDebt.getReadinessStatus()).isEqualTo(DebtEntity.ReadinessStatus.UNDER_APPEAL);
  }

  @Test
  void registerObjection_debtNotFound_throws() {
    when(debtRepository.existsById(DEBT_ID)).thenReturn(false);

    assertThatThrownBy(() -> service.registerObjection(DEBT_ID, DEBTOR_PERSON_ID, "disputed"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Debt not found");
  }

  @Test
  void registerObjection_activeAlreadyExists_throws() {
    when(debtRepository.existsById(DEBT_ID)).thenReturn(true);
    when(objectionRepository.existsByDebtIdAndStatus(DEBT_ID, ObjectionStatus.ACTIVE))
        .thenReturn(true);

    assertThatThrownBy(() -> service.registerObjection(DEBT_ID, DEBTOR_PERSON_ID, "disputed"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Active objection already exists");
  }

  @Test
  void resolveObjection_rejected_resumesCollection() {
    UUID objectionId = UUID.randomUUID();
    ObjectionEntity entity =
        ObjectionEntity.builder()
            .id(objectionId)
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_PERSON_ID)
            .reason("disputed")
            .status(ObjectionStatus.ACTIVE)
            .registeredAt(Instant.now())
            .build();

    when(objectionRepository.findById(objectionId)).thenReturn(Optional.of(entity));
    when(objectionRepository.save(any())).thenReturn(entity);
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(testDebt));
    when(debtRepository.save(any())).thenReturn(testDebt);

    ObjectionDto result =
        service.resolveObjection(objectionId, ObjectionStatus.REJECTED, "insufficient evidence");

    assertThat(result.getStatus()).isEqualTo("REJECTED");
    assertThat(result.getResolvedAt()).isNotNull();
    assertThat(result.getResolutionNote()).isEqualTo("insufficient evidence");
    assertThat(testDebt.getReadinessStatus())
        .isEqualTo(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION);
  }

  @Test
  void resolveObjection_upheld_keepsUnderAppeal() {
    UUID objectionId = UUID.randomUUID();
    ObjectionEntity entity =
        ObjectionEntity.builder()
            .id(objectionId)
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_PERSON_ID)
            .reason("disputed")
            .status(ObjectionStatus.ACTIVE)
            .registeredAt(Instant.now())
            .build();

    testDebt.setReadinessStatus(DebtEntity.ReadinessStatus.UNDER_APPEAL);
    when(objectionRepository.findById(objectionId)).thenReturn(Optional.of(entity));
    when(objectionRepository.save(any())).thenReturn(entity);
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(testDebt));
    when(debtRepository.save(any())).thenReturn(testDebt);

    ObjectionDto result =
        service.resolveObjection(objectionId, ObjectionStatus.UPHELD, "valid claim");

    assertThat(result.getStatus()).isEqualTo("UPHELD");
    assertThat(testDebt.getReadinessStatus()).isEqualTo(DebtEntity.ReadinessStatus.UNDER_APPEAL);
  }

  @Test
  void resolveObjection_alreadyResolved_throws() {
    UUID objectionId = UUID.randomUUID();
    ObjectionEntity entity =
        ObjectionEntity.builder()
            .id(objectionId)
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_PERSON_ID)
            .reason("disputed")
            .status(ObjectionStatus.REJECTED)
            .build();

    when(objectionRepository.findById(objectionId)).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> service.resolveObjection(objectionId, ObjectionStatus.UPHELD, "note"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already resolved");
  }

  @Test
  void resolveObjection_notFound_throws() {
    UUID objectionId = UUID.randomUUID();
    when(objectionRepository.findById(objectionId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.resolveObjection(objectionId, ObjectionStatus.REJECTED, "note"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Objection not found");
  }

  @Test
  void resolveObjection_activeOutcome_throws() {
    UUID objectionId = UUID.randomUUID();

    assertThatThrownBy(() -> service.resolveObjection(objectionId, ObjectionStatus.ACTIVE, "note"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("UPHELD or REJECTED");
  }

  @Test
  void hasActiveObjection_returnsTrue() {
    when(objectionRepository.existsByDebtIdAndStatus(DEBT_ID, ObjectionStatus.ACTIVE))
        .thenReturn(true);

    assertThat(service.hasActiveObjection(DEBT_ID)).isTrue();
  }

  @Test
  void hasActiveObjection_returnsFalse() {
    when(objectionRepository.existsByDebtIdAndStatus(DEBT_ID, ObjectionStatus.ACTIVE))
        .thenReturn(false);

    assertThat(service.hasActiveObjection(DEBT_ID)).isFalse();
  }

  @Test
  void getObjections_returnsList() {
    ObjectionEntity entity =
        ObjectionEntity.builder()
            .id(UUID.randomUUID())
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_PERSON_ID)
            .reason("disputed")
            .status(ObjectionStatus.ACTIVE)
            .registeredAt(Instant.now())
            .build();

    when(objectionRepository.findByDebtIdOrderByRegisteredAtDesc(DEBT_ID))
        .thenReturn(List.of(entity));

    List<ObjectionDto> result = service.getObjections(DEBT_ID);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getReason()).isEqualTo("disputed");
  }
}
