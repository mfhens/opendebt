package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.debtservice.dto.LiabilityDto;
import dk.ufst.opendebt.debtservice.entity.LiabilityEntity;
import dk.ufst.opendebt.debtservice.entity.LiabilityEntity.LiabilityType;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.LiabilityRepository;

@ExtendWith(MockitoExtension.class)
class LiabilityServiceImplTest {

  @Mock private LiabilityRepository liabilityRepository;
  @Mock private DebtRepository debtRepository;

  private LiabilityServiceImpl service;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final UUID DEBTOR_1 = UUID.randomUUID();
  private static final UUID DEBTOR_2 = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new LiabilityServiceImpl(liabilityRepository, debtRepository);
  }

  @Test
  void addLiability_sole_createsSuccessfully() {
    when(debtRepository.existsById(DEBT_ID)).thenReturn(true);
    when(liabilityRepository.existsByDebtIdAndDebtorPersonId(DEBT_ID, DEBTOR_1)).thenReturn(false);
    when(liabilityRepository.findByDebtIdAndActiveTrue(DEBT_ID)).thenReturn(List.of());
    when(liabilityRepository.save(any()))
        .thenAnswer(
            inv -> {
              LiabilityEntity e = inv.getArgument(0);
              e.setId(UUID.randomUUID());
              return e;
            });

    LiabilityDto result = service.addLiability(DEBT_ID, DEBTOR_1, LiabilityType.SOLE, null);

    assertThat(result.getLiabilityType()).isEqualTo("SOLE");
    assertThat(result.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(result.getDebtorPersonId()).isEqualTo(DEBTOR_1);
    assertThat(result.isActive()).isTrue();
  }

  @Test
  void addLiability_sole_rejectsSecondParty() {
    when(debtRepository.existsById(DEBT_ID)).thenReturn(true);
    when(liabilityRepository.existsByDebtIdAndDebtorPersonId(DEBT_ID, DEBTOR_2)).thenReturn(false);

    LiabilityEntity existing =
        LiabilityEntity.builder()
            .id(UUID.randomUUID())
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_1)
            .liabilityType(LiabilityType.SOLE)
            .active(true)
            .build();
    when(liabilityRepository.findByDebtIdAndActiveTrue(DEBT_ID)).thenReturn(List.of(existing));

    assertThatThrownBy(() -> service.addLiability(DEBT_ID, DEBTOR_2, LiabilityType.SOLE, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SOLE liability requires exactly one");
  }

  @Test
  void addLiability_jointAndSeveral_allowsMultipleParties() {
    when(debtRepository.existsById(DEBT_ID)).thenReturn(true);
    when(liabilityRepository.existsByDebtIdAndDebtorPersonId(DEBT_ID, DEBTOR_2)).thenReturn(false);

    LiabilityEntity existing =
        LiabilityEntity.builder()
            .id(UUID.randomUUID())
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_1)
            .liabilityType(LiabilityType.JOINT_AND_SEVERAL)
            .active(true)
            .build();
    when(liabilityRepository.findByDebtIdAndActiveTrue(DEBT_ID)).thenReturn(List.of(existing));
    when(liabilityRepository.save(any()))
        .thenAnswer(
            inv -> {
              LiabilityEntity e = inv.getArgument(0);
              e.setId(UUID.randomUUID());
              return e;
            });

    LiabilityDto result =
        service.addLiability(DEBT_ID, DEBTOR_2, LiabilityType.JOINT_AND_SEVERAL, null);

    assertThat(result.getLiabilityType()).isEqualTo("JOINT_AND_SEVERAL");
  }

  @Test
  void addLiability_proportional_validatesShares() {
    when(debtRepository.existsById(DEBT_ID)).thenReturn(true);
    when(liabilityRepository.existsByDebtIdAndDebtorPersonId(DEBT_ID, DEBTOR_1)).thenReturn(false);
    when(liabilityRepository.findByDebtIdAndActiveTrue(DEBT_ID)).thenReturn(List.of());
    when(liabilityRepository.save(any()))
        .thenAnswer(
            inv -> {
              LiabilityEntity e = inv.getArgument(0);
              e.setId(UUID.randomUUID());
              return e;
            });

    LiabilityDto result =
        service.addLiability(DEBT_ID, DEBTOR_1, LiabilityType.PROPORTIONAL, new BigDecimal("60"));

    assertThat(result.getLiabilityType()).isEqualTo("PROPORTIONAL");
    assertThat(result.getSharePercentage()).isEqualByComparingTo(new BigDecimal("60"));
  }

  @Test
  void addLiability_proportional_rejectsExceeding100Percent() {
    when(debtRepository.existsById(DEBT_ID)).thenReturn(true);
    when(liabilityRepository.existsByDebtIdAndDebtorPersonId(DEBT_ID, DEBTOR_2)).thenReturn(false);

    LiabilityEntity existing =
        LiabilityEntity.builder()
            .id(UUID.randomUUID())
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_1)
            .liabilityType(LiabilityType.PROPORTIONAL)
            .sharePercentage(new BigDecimal("70"))
            .active(true)
            .build();
    when(liabilityRepository.findByDebtIdAndActiveTrue(DEBT_ID)).thenReturn(List.of(existing));

    assertThatThrownBy(
            () ->
                service.addLiability(
                    DEBT_ID, DEBTOR_2, LiabilityType.PROPORTIONAL, new BigDecimal("40")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("exceed 100%");
  }

  @Test
  void addLiability_rejectsMixedTypes() {
    when(debtRepository.existsById(DEBT_ID)).thenReturn(true);
    when(liabilityRepository.existsByDebtIdAndDebtorPersonId(DEBT_ID, DEBTOR_2)).thenReturn(false);

    LiabilityEntity existing =
        LiabilityEntity.builder()
            .id(UUID.randomUUID())
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_1)
            .liabilityType(LiabilityType.JOINT_AND_SEVERAL)
            .active(true)
            .build();
    when(liabilityRepository.findByDebtIdAndActiveTrue(DEBT_ID)).thenReturn(List.of(existing));

    assertThatThrownBy(
            () ->
                service.addLiability(
                    DEBT_ID, DEBTOR_2, LiabilityType.PROPORTIONAL, new BigDecimal("50")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot mix liability types");
  }

  @Test
  void addLiability_debtNotFound_throws() {
    when(debtRepository.existsById(DEBT_ID)).thenReturn(false);

    assertThatThrownBy(() -> service.addLiability(DEBT_ID, DEBTOR_1, LiabilityType.SOLE, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Debt not found");
  }

  @Test
  void addLiability_duplicateDebtor_throws() {
    when(debtRepository.existsById(DEBT_ID)).thenReturn(true);
    when(liabilityRepository.existsByDebtIdAndDebtorPersonId(DEBT_ID, DEBTOR_1)).thenReturn(true);

    assertThatThrownBy(() -> service.addLiability(DEBT_ID, DEBTOR_1, LiabilityType.SOLE, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void removeLiability_deactivatesEntity() {
    UUID liabilityId = UUID.randomUUID();
    LiabilityEntity entity =
        LiabilityEntity.builder()
            .id(liabilityId)
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_1)
            .liabilityType(LiabilityType.SOLE)
            .active(true)
            .build();
    when(liabilityRepository.findById(liabilityId)).thenReturn(Optional.of(entity));
    when(liabilityRepository.save(any())).thenReturn(entity);

    service.removeLiability(liabilityId);

    assertThat(entity.isActive()).isFalse();
    verify(liabilityRepository).save(entity);
  }

  @Test
  void removeLiability_notFound_throws() {
    UUID liabilityId = UUID.randomUUID();
    when(liabilityRepository.findById(liabilityId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.removeLiability(liabilityId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Liability not found");
  }

  @Test
  void getLiabilities_returnsActiveLiabilities() {
    LiabilityEntity entity =
        LiabilityEntity.builder()
            .id(UUID.randomUUID())
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_1)
            .liabilityType(LiabilityType.SOLE)
            .active(true)
            .build();
    when(liabilityRepository.findByDebtIdAndActiveTrue(DEBT_ID)).thenReturn(List.of(entity));

    List<LiabilityDto> result = service.getLiabilities(DEBT_ID);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getLiabilityType()).isEqualTo("SOLE");
  }

  @Test
  void getDebtorLiabilities_returnsActiveLiabilities() {
    LiabilityEntity entity =
        LiabilityEntity.builder()
            .id(UUID.randomUUID())
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_1)
            .liabilityType(LiabilityType.JOINT_AND_SEVERAL)
            .active(true)
            .build();
    when(liabilityRepository.findByDebtorPersonIdAndActiveTrue(DEBTOR_1))
        .thenReturn(List.of(entity));

    List<LiabilityDto> result = service.getDebtorLiabilities(DEBTOR_1);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getLiabilityType()).isEqualTo("JOINT_AND_SEVERAL");
  }
}
