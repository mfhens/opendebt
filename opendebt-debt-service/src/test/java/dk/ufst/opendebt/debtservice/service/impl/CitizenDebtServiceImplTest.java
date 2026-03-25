package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import dk.ufst.opendebt.debtservice.dto.CitizenDebtSummaryResponse;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

@ExtendWith(MockitoExtension.class)
class CitizenDebtServiceImplTest {

  @Mock private DebtRepository debtRepository;

  @InjectMocks private CitizenDebtServiceImpl citizenDebtService;

  private static final UUID PERSON_ID = UUID.randomUUID();
  private DebtEntity debt1;
  private DebtEntity debt2;

  @BeforeEach
  void setUp() {
    debt1 =
        DebtEntity.builder()
            .id(UUID.randomUUID())
            .debtorPersonId(PERSON_ID)
            .creditorOrgId(UUID.randomUUID())
            .debtTypeCode("RESTSKAT")
            .principalAmount(new BigDecimal("10000"))
            .outstandingBalance(new BigDecimal("8000"))
            .interestAmount(new BigDecimal("500"))
            .feesAmount(new BigDecimal("100"))
            .dueDate(LocalDate.now().minusMonths(1))
            .status(DebtEntity.DebtStatus.ACTIVE)
            .lifecycleState(ClaimLifecycleState.RESTANCE)
            .build();

    debt2 =
        DebtEntity.builder()
            .id(UUID.randomUUID())
            .debtorPersonId(PERSON_ID)
            .creditorOrgId(UUID.randomUUID())
            .debtTypeCode("MOMSGAELD")
            .principalAmount(new BigDecimal("5000"))
            .outstandingBalance(new BigDecimal("5000"))
            .interestAmount(new BigDecimal("200"))
            .feesAmount(new BigDecimal("50"))
            .dueDate(LocalDate.now().minusDays(15))
            .status(DebtEntity.DebtStatus.ACTIVE)
            .lifecycleState(ClaimLifecycleState.REGISTERED)
            .build();
  }

  @Test
  void getDebtSummary_noStatusFilter_returnsAllDebts() {
    Pageable pageable = PageRequest.of(0, 20);
    when(debtRepository.findByDebtorPersonId(PERSON_ID)).thenReturn(List.of(debt1, debt2));

    CitizenDebtSummaryResponse response =
        citizenDebtService.getDebtSummary(PERSON_ID, null, pageable);

    assertThat(response.getDebts()).hasSize(2);
    assertThat(response.getTotalDebtCount()).isEqualTo(2);
    assertThat(response.getTotalOutstandingAmount()).isEqualByComparingTo(new BigDecimal("13000"));
    assertThat(response.getPageNumber()).isZero();
    assertThat(response.getPageSize()).isEqualTo(20);
  }

  @Test
  void getDebtSummary_withStatusFilter_returnsFilteredDebts() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<DebtEntity> page = new PageImpl<>(List.of(debt1), pageable, 1);
    when(debtRepository.findByFilters(
            eq(null), eq(PERSON_ID), eq(DebtEntity.DebtStatus.ACTIVE), eq(null), any()))
        .thenReturn(page);
    when(debtRepository.findByDebtorPersonId(PERSON_ID)).thenReturn(List.of(debt1, debt2));

    CitizenDebtSummaryResponse response =
        citizenDebtService.getDebtSummary(PERSON_ID, DebtEntity.DebtStatus.ACTIVE, pageable);

    assertThat(response.getDebts()).hasSize(1);
    assertThat(response.getTotalDebtCount()).isEqualTo(2); // Total across ALL debts
    assertThat(response.getTotalOutstandingAmount())
        .isEqualByComparingTo(new BigDecimal("13000")); // Total across ALL debts
  }

  @Test
  void getDebtSummary_pagination_returnsCorrectPage() {
    Pageable pageable = PageRequest.of(0, 1); // Page 0, size 1
    when(debtRepository.findByDebtorPersonId(PERSON_ID)).thenReturn(List.of(debt1, debt2));

    CitizenDebtSummaryResponse response =
        citizenDebtService.getDebtSummary(PERSON_ID, null, pageable);

    assertThat(response.getDebts()).hasSize(1); // Only 1 debt on this page
    assertThat(response.getTotalDebtCount()).isEqualTo(2); // Total count
    assertThat(response.getTotalPages()).isEqualTo(2); // 2 total pages
    assertThat(response.getTotalElements()).isEqualTo(2);
  }

  @Test
  void getDebtSummary_noDebts_returnsEmptyList() {
    Pageable pageable = PageRequest.of(0, 20);
    when(debtRepository.findByDebtorPersonId(PERSON_ID)).thenReturn(List.of());

    CitizenDebtSummaryResponse response =
        citizenDebtService.getDebtSummary(PERSON_ID, null, pageable);

    assertThat(response.getDebts()).isEmpty();
    assertThat(response.getTotalDebtCount()).isZero();
    assertThat(response.getTotalOutstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void mapToCitizenDto_doesNotIncludePii() {
    Pageable pageable = PageRequest.of(0, 20);
    when(debtRepository.findByDebtorPersonId(PERSON_ID)).thenReturn(List.of(debt1));

    CitizenDebtSummaryResponse response =
        citizenDebtService.getDebtSummary(PERSON_ID, null, pageable);

    assertThat(response.getDebts()).hasSize(1);
    var debtItem = response.getDebts().get(0);
    // Verify no PII fields (person names, addresses, etc.)
    assertThat(debtItem.getDebtId()).isNotNull();
    assertThat(debtItem.getDebtTypeCode()).isEqualTo("RESTSKAT");
    assertThat(debtItem.getDebtTypeName()).isEqualTo("Restskat");
    assertThat(debtItem.getPrincipalAmount()).isEqualByComparingTo(new BigDecimal("10000"));
    assertThat(debtItem.getStatus()).isEqualTo("ACTIVE");
    assertThat(debtItem.getLifecycleState()).isEqualTo("RESTANCE");
    // No creditor_org_id, no readiness_status, no internal fields
  }

  @Test
  void getDebtSummary_handlesNullAmounts() {
    DebtEntity debtWithNulls =
        DebtEntity.builder()
            .id(UUID.randomUUID())
            .debtorPersonId(PERSON_ID)
            .creditorOrgId(UUID.randomUUID())
            .debtTypeCode("STUDIEGAELD")
            .principalAmount(new BigDecimal("2000"))
            .outstandingBalance(null) // Null balance
            .interestAmount(null)
            .feesAmount(null)
            .dueDate(LocalDate.now())
            .status(DebtEntity.DebtStatus.ACTIVE)
            .build();

    Pageable pageable = PageRequest.of(0, 20);
    when(debtRepository.findByDebtorPersonId(PERSON_ID)).thenReturn(List.of(debtWithNulls));

    CitizenDebtSummaryResponse response =
        citizenDebtService.getDebtSummary(PERSON_ID, null, pageable);

    assertThat(response.getTotalOutstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(response.getDebts()).hasSize(1);
  }
}
