package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.debtservice.dto.ActiveFordringResponseDto;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.CollectionMeasureRepository;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

@ExtendWith(MockitoExtension.class)
class ActiveFordringServiceImplTest {

  @Mock private DebtRepository debtRepository;
  @Mock private CollectionMeasureRepository collectionMeasureRepository;

  private ActiveFordringServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new ActiveFordringServiceImpl(debtRepository, collectionMeasureRepository);
  }

  @Test
  void getActiveFordringer_returnsEmptyListWhenNoActiveDebts() {
    UUID debtorId = UUID.randomUUID();
    when(debtRepository.findActiveFordringerByDebtorPersonId(debtorId))
        .thenReturn(Collections.emptyList());

    List<ActiveFordringResponseDto> result = service.getActiveFordringer(debtorId);

    assertThat(result).isEmpty();
    verifyNoInteractions(collectionMeasureRepository);
  }

  @Test
  void getActiveFordringer_mapsAllFieldsCorrectly() {
    UUID debtorId = UUID.randomUUID();
    UUID fordringId = UUID.randomUUID();
    UUID creditorId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();
    LocalDateTime receivedAt = LocalDateTime.of(2025, 6, 1, 9, 0);

    DebtEntity entity =
        DebtEntity.builder()
            .id(fordringId)
            .debtorPersonId(debtorId)
            .creditorOrgId(creditorId)
            .debtTypeCode("600")
            .outstandingBalance(new BigDecimal("12000.00"))
            .feesAmount(new BigDecimal("300.00"))
            .beloebOpkraevningsrenter(new BigDecimal("150.00"))
            .beloebInddrivelsesrenterFordringshaver(new BigDecimal("50.00"))
            .beloebInddrivelsesrenterFoerTilbagefoersel(new BigDecimal("25.00"))
            .beloebInddrivelsesrenterStk1(new BigDecimal("75.00"))
            .beloebOevrigeRenterPsrm(new BigDecimal("10.00"))
            .sekvensNummer(3)
            .gilParagraf("GIL § 4, stk. 1")
            .parentClaimId(parentId)
            .receivedAt(receivedAt)
            .status(DebtEntity.DebtStatus.ACTIVE)
            .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
            .principalAmount(new BigDecimal("12000.00"))
            .dueDate(LocalDate.of(2025, 12, 1))
            .build();

    when(debtRepository.findActiveFordringerByDebtorPersonId(debtorId)).thenReturn(List.of(entity));
    when(collectionMeasureRepository.findActiveWageGarnishmentDebtIds(Set.of(fordringId)))
        .thenReturn(Set.of(fordringId)); // this one IS in lønindeholdelse

    List<ActiveFordringResponseDto> result = service.getActiveFordringer(debtorId);

    assertThat(result).hasSize(1);
    ActiveFordringResponseDto dto = result.get(0);
    assertThat(dto.getFordringId()).isEqualTo(fordringId);
    assertThat(dto.getFordringType()).isEqualTo("600");
    assertThat(dto.getBeloebResterende()).isEqualByComparingTo("12000.00");
    assertThat(dto.getOpkraevningsrenter()).isEqualByComparingTo("150.00");
    assertThat(dto.getInddrivelsesrenterFordringshaver()).isEqualByComparingTo("50.00");
    assertThat(dto.getInddrivelsesrenterFoerTilbagefoersel()).isEqualByComparingTo("25.00");
    assertThat(dto.getInddrivelsesrenterStk1()).isEqualByComparingTo("75.00");
    assertThat(dto.getOevrigeRenterPsrm()).isEqualByComparingTo("10.00");
    assertThat(dto.getInddrivelsesomkostninger()).isEqualByComparingTo("300.00");
    assertThat(dto.getSekvensNummer()).isEqualTo(3);
    assertThat(dto.getInLoenindeholdelsesIndsats()).isTrue();
    assertThat(dto.getOpskrivningAfFordringId()).isEqualTo(parentId);
    assertThat(dto.getFordringshaverId()).isEqualTo(creditorId);
    assertThat(dto.getGilParagraf()).isEqualTo("GIL § 4, stk. 1");
    assertThat(dto.getApplicationTimestamp()).isEqualTo(receivedAt.atOffset(ZoneOffset.UTC));
  }

  @Test
  void getActiveFordringer_setsInLoenFalseWhenNotInWageGarnishment() {
    UUID debtorId = UUID.randomUUID();
    UUID fordringId = UUID.randomUUID();
    DebtEntity entity = minimalActiveDebt(fordringId, debtorId);

    when(debtRepository.findActiveFordringerByDebtorPersonId(debtorId)).thenReturn(List.of(entity));
    when(collectionMeasureRepository.findActiveWageGarnishmentDebtIds(anySet()))
        .thenReturn(Collections.emptySet()); // none in lønindeholdelse

    List<ActiveFordringResponseDto> result = service.getActiveFordringer(debtorId);

    assertThat(result.get(0).getInLoenindeholdelsesIndsats()).isFalse();
  }

  @Test
  void getActiveFordringer_handlesNullReceivedAt() {
    UUID debtorId = UUID.randomUUID();
    UUID fordringId = UUID.randomUUID();
    DebtEntity entity = minimalActiveDebt(fordringId, debtorId);
    entity.setReceivedAt(null);

    when(debtRepository.findActiveFordringerByDebtorPersonId(debtorId)).thenReturn(List.of(entity));
    when(collectionMeasureRepository.findActiveWageGarnishmentDebtIds(anySet()))
        .thenReturn(Collections.emptySet());

    List<ActiveFordringResponseDto> result = service.getActiveFordringer(debtorId);

    assertThat(result.get(0).getApplicationTimestamp()).isNull();
  }

  @Test
  void getActiveFordringer_batchesWageGarnishmentLookup() {
    UUID debtorId = UUID.randomUUID();
    DebtEntity d1 = minimalActiveDebt(UUID.randomUUID(), debtorId);
    DebtEntity d2 = minimalActiveDebt(UUID.randomUUID(), debtorId);

    when(debtRepository.findActiveFordringerByDebtorPersonId(debtorId)).thenReturn(List.of(d1, d2));
    when(collectionMeasureRepository.findActiveWageGarnishmentDebtIds(anySet()))
        .thenReturn(Collections.emptySet());

    service.getActiveFordringer(debtorId);

    // Batch query must be called exactly once, regardless of number of fordringer
    verify(collectionMeasureRepository, times(1)).findActiveWageGarnishmentDebtIds(anySet());
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private DebtEntity minimalActiveDebt(UUID id, UUID debtorId) {
    return DebtEntity.builder()
        .id(id)
        .debtorPersonId(debtorId)
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("700")
        .outstandingBalance(new BigDecimal("1000.00"))
        .status(DebtEntity.DebtStatus.ACTIVE)
        .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
        .principalAmount(new BigDecimal("1000.00"))
        .dueDate(LocalDate.of(2026, 1, 1))
        .receivedAt(LocalDateTime.of(2025, 1, 1, 0, 0))
        .build();
  }
}
