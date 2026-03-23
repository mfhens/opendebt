package dk.ufst.opendebt.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import dk.ufst.opendebt.common.dto.DebtEventDto;
import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;
import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;
import dk.ufst.opendebt.payment.bookkeeping.repository.DebtEventRepository;
import dk.ufst.opendebt.payment.bookkeeping.repository.LedgerEntryRepository;
import dk.ufst.opendebt.payment.client.CaseServiceClient;
import dk.ufst.opendebt.payment.dto.LedgerEntryDto;
import dk.ufst.opendebt.payment.dto.LedgerSummaryDto;

@ExtendWith(MockitoExtension.class)
class LedgerQueryServiceImplTest {

  @Mock private LedgerEntryRepository ledgerEntryRepository;
  @Mock private DebtEventRepository debtEventRepository;
  @Mock private CaseServiceClient caseServiceClient;

  @InjectMocks private LedgerQueryServiceImpl service;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final UUID CASE_ID = UUID.randomUUID();
  private static final UUID TRANSACTION_ID = UUID.randomUUID();
  private static final LocalDate TODAY = LocalDate.now();

  // --- getLedgerEntriesByDebtId ---

  @Test
  void getLedgerEntriesByDebtId_delegatesToRepositoryAndMaps() {
    Pageable pageable = PageRequest.of(0, 20);
    LedgerEntryEntity entity = buildLedgerEntry(LedgerEntryEntity.EntryCategory.PAYMENT);
    Page<LedgerEntryEntity> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

    when(ledgerEntryRepository.findByDebtIdFiltered(
            eq(DEBT_ID), isNull(), isNull(), isNull(), eq(true), eq(pageable)))
        .thenReturn(entityPage);

    Page<LedgerEntryDto> result =
        service.getLedgerEntriesByDebtId(DEBT_ID, null, null, null, true, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    LedgerEntryDto dto = result.getContent().get(0);
    assertThat(dto.getId()).isEqualTo(entity.getId());
    assertThat(dto.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(dto.getEntryType()).isEqualTo("CREDIT");
    assertThat(dto.getEntryCategory()).isEqualTo("PAYMENT");
    assertThat(dto.getAmount()).isEqualByComparingTo("500.00");
    verify(ledgerEntryRepository).findByDebtIdFiltered(DEBT_ID, null, null, null, true, pageable);
  }

  @Test
  void getLedgerEntriesByDebtId_withFilters_passesParametersThrough() {
    Pageable pageable = PageRequest.of(0, 10);
    LocalDate from = TODAY.minusDays(30);
    LocalDate to = TODAY;
    LedgerEntryEntity.EntryCategory category = LedgerEntryEntity.EntryCategory.INTEREST_ACCRUAL;

    when(ledgerEntryRepository.findByDebtIdFiltered(
            eq(DEBT_ID), eq(from), eq(to), eq(category), eq(false), eq(pageable)))
        .thenReturn(Page.empty(pageable));

    Page<LedgerEntryDto> result =
        service.getLedgerEntriesByDebtId(DEBT_ID, from, to, category, false, pageable);

    assertThat(result.getTotalElements()).isZero();
    verify(ledgerEntryRepository)
        .findByDebtIdFiltered(DEBT_ID, from, to, category, false, pageable);
  }

  // --- getLedgerEntriesByCaseId ---

  @Test
  void getLedgerEntriesByCaseId_callsCaseServiceClientThenQueriesByDebtIds() {
    Pageable pageable = PageRequest.of(0, 20);
    UUID debtId1 = UUID.randomUUID();
    UUID debtId2 = UUID.randomUUID();
    List<UUID> debtIds = List.of(debtId1, debtId2);

    when(caseServiceClient.getDebtIdsForCase(CASE_ID)).thenReturn(debtIds);
    when(ledgerEntryRepository.findByDebtIdsFiltered(
            eq(debtIds), isNull(), isNull(), isNull(), eq(true), eq(pageable)))
        .thenReturn(Page.empty(pageable));

    Page<LedgerEntryDto> result =
        service.getLedgerEntriesByCaseId(CASE_ID, null, null, null, true, pageable);

    assertThat(result.getTotalElements()).isZero();
    verify(caseServiceClient).getDebtIdsForCase(CASE_ID);
    verify(ledgerEntryRepository).findByDebtIdsFiltered(debtIds, null, null, null, true, pageable);
  }

  @Test
  void getLedgerEntriesByCaseId_emptyDebtIds_returnsEmptyPage() {
    Pageable pageable = PageRequest.of(0, 20);
    when(caseServiceClient.getDebtIdsForCase(CASE_ID)).thenReturn(List.of());

    Page<LedgerEntryDto> result =
        service.getLedgerEntriesByCaseId(CASE_ID, null, null, null, true, pageable);

    assertThat(result.getTotalElements()).isZero();
    verify(caseServiceClient).getDebtIdsForCase(CASE_ID);
    verifyNoInteractions(ledgerEntryRepository);
  }

  // --- getEventsByDebtId ---

  @Test
  void getEventsByDebtId_returnsMappedEvents() {
    DebtEventEntity event = buildDebtEvent(DebtEventEntity.EventType.PAYMENT_RECEIVED);

    when(debtEventRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(event));

    List<DebtEventDto> result = service.getEventsByDebtId(DEBT_ID);

    assertThat(result).hasSize(1);
    DebtEventDto dto = result.get(0);
    assertThat(dto.getId()).isEqualTo(event.getId());
    assertThat(dto.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(dto.getEventType()).isEqualTo("PAYMENT_RECEIVED");
    assertThat(dto.getAmount()).isEqualByComparingTo("500.00");
  }

  // --- getEventsByCaseId ---

  @Test
  void getEventsByCaseId_mergesAndSortsEventsAcrossDebts() {
    UUID debtId1 = UUID.randomUUID();
    UUID debtId2 = UUID.randomUUID();

    DebtEventEntity event1 =
        DebtEventEntity.builder()
            .id(UUID.randomUUID())
            .debtId(debtId1)
            .eventType(DebtEventEntity.EventType.DEBT_REGISTERED)
            .effectiveDate(TODAY.minusDays(10))
            .amount(new BigDecimal("1000.00"))
            .createdAt(LocalDateTime.now().minusDays(10))
            .build();

    DebtEventEntity event2 =
        DebtEventEntity.builder()
            .id(UUID.randomUUID())
            .debtId(debtId2)
            .eventType(DebtEventEntity.EventType.PAYMENT_RECEIVED)
            .effectiveDate(TODAY.minusDays(5))
            .amount(new BigDecimal("200.00"))
            .createdAt(LocalDateTime.now().minusDays(5))
            .build();

    when(caseServiceClient.getDebtIdsForCase(CASE_ID)).thenReturn(List.of(debtId1, debtId2));
    when(debtEventRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(debtId1))
        .thenReturn(List.of(event1));
    when(debtEventRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(debtId2))
        .thenReturn(List.of(event2));

    List<DebtEventDto> result = service.getEventsByCaseId(CASE_ID);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getEventType()).isEqualTo("DEBT_REGISTERED");
    assertThat(result.get(1).getEventType()).isEqualTo("PAYMENT_RECEIVED");
  }

  @Test
  void getEventsByCaseId_emptyDebtIds_returnsEmptyList() {
    when(caseServiceClient.getDebtIdsForCase(CASE_ID)).thenReturn(List.of());

    List<DebtEventDto> result = service.getEventsByCaseId(CASE_ID);

    assertThat(result).isEmpty();
    verifyNoInteractions(debtEventRepository);
  }

  // --- getLedgerSummary ---

  @Test
  void getLedgerSummary_computesBalancesCorrectly() {
    LedgerEntryEntity debtRegistration =
        LedgerEntryEntity.builder()
            .id(UUID.randomUUID())
            .transactionId(UUID.randomUUID())
            .debtId(DEBT_ID)
            .accountCode("1000")
            .accountName("Debtor Account")
            .entryType(LedgerEntryEntity.EntryType.DEBIT)
            .amount(new BigDecimal("10000.00"))
            .effectiveDate(TODAY.minusDays(30))
            .postingDate(TODAY.minusDays(30))
            .entryCategory(LedgerEntryEntity.EntryCategory.DEBT_REGISTRATION)
            .createdAt(LocalDateTime.now().minusDays(30))
            .build();

    LedgerEntryEntity payment =
        LedgerEntryEntity.builder()
            .id(UUID.randomUUID())
            .transactionId(UUID.randomUUID())
            .debtId(DEBT_ID)
            .accountCode("1000")
            .accountName("Debtor Account")
            .entryType(LedgerEntryEntity.EntryType.CREDIT)
            .amount(new BigDecimal("3000.00"))
            .effectiveDate(TODAY.minusDays(20))
            .postingDate(TODAY.minusDays(20))
            .entryCategory(LedgerEntryEntity.EntryCategory.PAYMENT)
            .createdAt(LocalDateTime.now().minusDays(20))
            .build();

    LedgerEntryEntity interest =
        LedgerEntryEntity.builder()
            .id(UUID.randomUUID())
            .transactionId(UUID.randomUUID())
            .debtId(DEBT_ID)
            .accountCode("2000")
            .accountName("Interest Account")
            .entryType(LedgerEntryEntity.EntryType.DEBIT)
            .amount(new BigDecimal("150.00"))
            .effectiveDate(TODAY.minusDays(10))
            .postingDate(TODAY.minusDays(10))
            .entryCategory(LedgerEntryEntity.EntryCategory.INTEREST_ACCRUAL)
            .createdAt(LocalDateTime.now().minusDays(10))
            .build();

    LedgerEntryEntity writeOff =
        LedgerEntryEntity.builder()
            .id(UUID.randomUUID())
            .transactionId(UUID.randomUUID())
            .debtId(DEBT_ID)
            .accountCode("1000")
            .accountName("Debtor Account")
            .entryType(LedgerEntryEntity.EntryType.CREDIT)
            .amount(new BigDecimal("500.00"))
            .effectiveDate(TODAY.minusDays(5))
            .postingDate(TODAY.minusDays(5))
            .entryCategory(LedgerEntryEntity.EntryCategory.WRITE_OFF)
            .createdAt(LocalDateTime.now().minusDays(5))
            .build();

    LedgerEntryEntity storno =
        LedgerEntryEntity.builder()
            .id(UUID.randomUUID())
            .transactionId(UUID.randomUUID())
            .debtId(DEBT_ID)
            .accountCode("1000")
            .accountName("Debtor Account")
            .entryType(LedgerEntryEntity.EntryType.CREDIT)
            .amount(new BigDecimal("100.00"))
            .effectiveDate(TODAY)
            .postingDate(TODAY)
            .entryCategory(LedgerEntryEntity.EntryCategory.STORNO)
            .createdAt(LocalDateTime.now())
            .build();

    when(ledgerEntryRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(debtRegistration, payment, interest, writeOff, storno));

    LedgerSummaryDto summary = service.getLedgerSummary(DEBT_ID);

    assertThat(summary.getDebtId()).isEqualTo(DEBT_ID);
    // Principal: 10000 (DEBIT) - 3000 (CREDIT/PAYMENT) - 500 (CREDIT/WRITE_OFF) - 100
    // (CREDIT/STORNO) = 6400
    assertThat(summary.getPrincipalBalance()).isEqualByComparingTo("6400.00");
    // Interest: 150 (DEBIT)
    assertThat(summary.getInterestBalance()).isEqualByComparingTo("150.00");
    // Total: 6400 + 150 = 6550
    assertThat(summary.getTotalBalance()).isEqualByComparingTo("6550.00");
    assertThat(summary.getTotalPayments()).isEqualByComparingTo("3000.00");
    assertThat(summary.getTotalInterestAccrued()).isEqualByComparingTo("150.00");
    assertThat(summary.getTotalWriteOffs()).isEqualByComparingTo("500.00");
    assertThat(summary.getTotalCorrections()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(summary.getEntryCount()).isEqualTo(5);
    assertThat(summary.getStornoCount()).isEqualTo(1);
    assertThat(summary.getLastEventDate()).isEqualTo(TODAY);
    assertThat(summary.getLastPostingDate()).isEqualTo(TODAY);
  }

  @Test
  void getLedgerSummary_emptyEntries_returnsZeroBalances() {
    when(ledgerEntryRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of());

    LedgerSummaryDto summary = service.getLedgerSummary(DEBT_ID);

    assertThat(summary.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(summary.getPrincipalBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(summary.getInterestBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(summary.getTotalBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(summary.getEntryCount()).isZero();
    assertThat(summary.getStornoCount()).isZero();
    assertThat(summary.getLastEventDate()).isNull();
    assertThat(summary.getLastPostingDate()).isNull();
  }

  // --- Entity-to-DTO mapping ---

  @Test
  void entityToDtoMapping_preservesAllFields() {
    Pageable pageable = PageRequest.of(0, 20);
    UUID reversalTxId = UUID.randomUUID();
    LedgerEntryEntity entity =
        LedgerEntryEntity.builder()
            .id(UUID.randomUUID())
            .transactionId(TRANSACTION_ID)
            .debtId(DEBT_ID)
            .accountCode("3000")
            .accountName("Test Account")
            .entryType(LedgerEntryEntity.EntryType.DEBIT)
            .amount(new BigDecimal("999.99"))
            .effectiveDate(TODAY)
            .postingDate(TODAY.plusDays(1))
            .reference("REF-001")
            .description("Test entry")
            .entryCategory(LedgerEntryEntity.EntryCategory.CORRECTION)
            .reversalOfTransactionId(reversalTxId)
            .createdAt(LocalDateTime.of(2025, 1, 1, 12, 0))
            .build();

    when(ledgerEntryRepository.findByDebtIdFiltered(
            eq(DEBT_ID), isNull(), isNull(), isNull(), eq(true), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

    Page<LedgerEntryDto> result =
        service.getLedgerEntriesByDebtId(DEBT_ID, null, null, null, true, pageable);

    LedgerEntryDto dto = result.getContent().get(0);
    assertThat(dto.getId()).isEqualTo(entity.getId());
    assertThat(dto.getTransactionId()).isEqualTo(TRANSACTION_ID);
    assertThat(dto.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(dto.getAccountCode()).isEqualTo("3000");
    assertThat(dto.getAccountName()).isEqualTo("Test Account");
    assertThat(dto.getEntryType()).isEqualTo("DEBIT");
    assertThat(dto.getAmount()).isEqualByComparingTo("999.99");
    assertThat(dto.getEffectiveDate()).isEqualTo(TODAY);
    assertThat(dto.getPostingDate()).isEqualTo(TODAY.plusDays(1));
    assertThat(dto.getReference()).isEqualTo("REF-001");
    assertThat(dto.getDescription()).isEqualTo("Test entry");
    assertThat(dto.getEntryCategory()).isEqualTo("CORRECTION");
    assertThat(dto.getReversalOfTransactionId()).isEqualTo(reversalTxId);
    assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 12, 0));
  }

  // --- Helper methods ---

  private LedgerEntryEntity buildLedgerEntry(LedgerEntryEntity.EntryCategory category) {
    return LedgerEntryEntity.builder()
        .id(UUID.randomUUID())
        .transactionId(TRANSACTION_ID)
        .debtId(DEBT_ID)
        .accountCode("1000")
        .accountName("Debtor Account")
        .entryType(LedgerEntryEntity.EntryType.CREDIT)
        .amount(new BigDecimal("500.00"))
        .effectiveDate(TODAY)
        .postingDate(TODAY)
        .entryCategory(category)
        .createdAt(LocalDateTime.now())
        .build();
  }

  private DebtEventEntity buildDebtEvent(DebtEventEntity.EventType eventType) {
    return DebtEventEntity.builder()
        .id(UUID.randomUUID())
        .debtId(DEBT_ID)
        .eventType(eventType)
        .effectiveDate(TODAY)
        .amount(new BigDecimal("500.00"))
        .createdAt(LocalDateTime.now())
        .build();
  }
}
