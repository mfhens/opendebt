package dk.ufst.opendebt.payment.bookkeeping.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;
import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;
import dk.ufst.opendebt.payment.bookkeeping.repository.DebtEventRepository;
import dk.ufst.opendebt.payment.bookkeeping.repository.LedgerEntryRepository;

@ExtendWith(MockitoExtension.class)
class BookkeepingServiceImplTest {

  @Mock private LedgerEntryRepository ledgerEntryRepository;
  @Mock private DebtEventRepository debtEventRepository;

  private BookkeepingServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new BookkeepingServiceImpl(ledgerEntryRepository, debtEventRepository);
  }

  @Test
  void recordDebtRegistered_postsDebitAndCreditWithCorrectAccounts() {
    UUID debtId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("50000.00");
    LocalDate effectiveDate = LocalDate.of(2025, 10, 1);

    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(debtEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    service.recordDebtRegistered(debtId, amount, effectiveDate, "REF-001");

    ArgumentCaptor<LedgerEntryEntity> captor = forClass(LedgerEntryEntity.class);
    verify(ledgerEntryRepository, times(2)).save(captor.capture());

    LedgerEntryEntity debit = captor.getAllValues().get(0);
    LedgerEntryEntity credit = captor.getAllValues().get(1);

    assertThat(debit.getAccountCode()).isEqualTo("1000");
    assertThat(debit.getEntryType()).isEqualTo(LedgerEntryEntity.EntryType.DEBIT);
    assertThat(debit.getAmount()).isEqualByComparingTo(amount);
    assertThat(debit.getEffectiveDate()).isEqualTo(effectiveDate);
    assertThat(debit.getEntryCategory())
        .isEqualTo(LedgerEntryEntity.EntryCategory.DEBT_REGISTRATION);

    assertThat(credit.getAccountCode()).isEqualTo("3000");
    assertThat(credit.getEntryType()).isEqualTo(LedgerEntryEntity.EntryType.CREDIT);
    assertThat(credit.getAmount()).isEqualByComparingTo(amount);

    assertThat(debit.getTransactionId()).isEqualTo(credit.getTransactionId());
  }

  @Test
  void recordDebtRegistered_recordsEventInTimeline() {
    UUID debtId = UUID.randomUUID();
    LocalDate effectiveDate = LocalDate.of(2025, 10, 1);

    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(debtEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    service.recordDebtRegistered(debtId, new BigDecimal("10000"), effectiveDate, "REF-001");

    ArgumentCaptor<DebtEventEntity> captor = forClass(DebtEventEntity.class);
    verify(debtEventRepository).save(captor.capture());

    DebtEventEntity event = captor.getValue();
    assertThat(event.getDebtId()).isEqualTo(debtId);
    assertThat(event.getEventType()).isEqualTo(DebtEventEntity.EventType.DEBT_REGISTERED);
    assertThat(event.getEffectiveDate()).isEqualTo(effectiveDate);
    assertThat(event.getAmount()).isEqualByComparingTo("10000");
    assertThat(event.getLedgerTransactionId()).isNotNull();
  }

  @Test
  void recordPaymentReceived_postsSkbBankDebitAndReceivablesCredit() {
    UUID debtId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("5000.00");
    LocalDate effectiveDate = LocalDate.of(2025, 11, 15);

    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(debtEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    service.recordPaymentReceived(debtId, amount, effectiveDate, "CREMUL-001");

    ArgumentCaptor<LedgerEntryEntity> captor = forClass(LedgerEntryEntity.class);
    verify(ledgerEntryRepository, times(2)).save(captor.capture());

    LedgerEntryEntity debit = captor.getAllValues().get(0);
    LedgerEntryEntity credit = captor.getAllValues().get(1);

    assertThat(debit.getAccountCode()).isEqualTo("2000"); // SKB Bank
    assertThat(debit.getEntryType()).isEqualTo(LedgerEntryEntity.EntryType.DEBIT);
    assertThat(credit.getAccountCode()).isEqualTo("1000"); // Fordringer
    assertThat(credit.getEntryType()).isEqualTo(LedgerEntryEntity.EntryType.CREDIT);
    assertThat(debit.getEntryCategory()).isEqualTo(LedgerEntryEntity.EntryCategory.PAYMENT);
  }

  @Test
  void recordInterestAccrued_postsInterestReceivableDebitAndRevenueCredit() {
    UUID debtId = UUID.randomUUID();
    BigDecimal interest = new BigDecimal("821.92");
    LocalDate effectiveDate = LocalDate.of(2025, 12, 31);

    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(debtEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    service.recordInterestAccrued(debtId, interest, effectiveDate, "INT-Q4");

    ArgumentCaptor<LedgerEntryEntity> captor = forClass(LedgerEntryEntity.class);
    verify(ledgerEntryRepository, times(2)).save(captor.capture());

    LedgerEntryEntity debit = captor.getAllValues().get(0);
    LedgerEntryEntity credit = captor.getAllValues().get(1);

    assertThat(debit.getAccountCode()).isEqualTo("1100"); // Renter tilgodehavende
    assertThat(credit.getAccountCode()).isEqualTo("3100"); // Renteindtaegter
    assertThat(debit.getEntryCategory())
        .isEqualTo(LedgerEntryEntity.EntryCategory.INTEREST_ACCRUAL);
  }

  @Test
  void recordOffsetting_postsModregningDebitAndReceivablesCredit() {
    UUID debtId = UUID.randomUUID();

    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(debtEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    service.recordOffsetting(debtId, new BigDecimal("3000"), LocalDate.now(), "MOD-001");

    ArgumentCaptor<LedgerEntryEntity> captor = forClass(LedgerEntryEntity.class);
    verify(ledgerEntryRepository, times(2)).save(captor.capture());

    assertThat(captor.getAllValues().get(0).getAccountCode()).isEqualTo("5000"); // Modregning
    assertThat(captor.getAllValues().get(1).getAccountCode()).isEqualTo("1000"); // Fordringer
  }

  @Test
  void recordWriteOff_postsExpenseDebitAndReceivablesCredit() {
    UUID debtId = UUID.randomUUID();

    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(debtEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    service.recordWriteOff(debtId, new BigDecimal("15000"), LocalDate.now(), "WO-001");

    ArgumentCaptor<LedgerEntryEntity> captor = forClass(LedgerEntryEntity.class);
    verify(ledgerEntryRepository, times(2)).save(captor.capture());

    assertThat(captor.getAllValues().get(0).getAccountCode()).isEqualTo("4000"); // Tab
    assertThat(captor.getAllValues().get(1).getAccountCode()).isEqualTo("1000"); // Fordringer
  }

  @Test
  void recordRefund_postsReceivablesDebitAndBankCredit() {
    UUID debtId = UUID.randomUUID();

    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(debtEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    service.recordRefund(debtId, new BigDecimal("2000"), LocalDate.now(), "DEBMUL-001");

    ArgumentCaptor<LedgerEntryEntity> captor = forClass(LedgerEntryEntity.class);
    verify(ledgerEntryRepository, times(2)).save(captor.capture());

    assertThat(captor.getAllValues().get(0).getAccountCode()).isEqualTo("1000"); // Fordringer
    assertThat(captor.getAllValues().get(1).getAccountCode()).isEqualTo("2000"); // SKB Bank
    assertThat(captor.getAllValues().get(0).getEntryCategory())
        .isEqualTo(LedgerEntryEntity.EntryCategory.REFUND);
  }

  @Test
  void allEntries_havePostingDateSetToToday() {
    UUID debtId = UUID.randomUUID();
    LocalDate effectiveDate = LocalDate.of(2025, 6, 1); // In the past

    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(debtEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    service.recordDebtRegistered(debtId, new BigDecimal("1000"), effectiveDate, "REF");

    ArgumentCaptor<LedgerEntryEntity> captor = forClass(LedgerEntryEntity.class);
    verify(ledgerEntryRepository, times(2)).save(captor.capture());

    for (LedgerEntryEntity entry : captor.getAllValues()) {
      assertThat(entry.getPostingDate()).isEqualTo(LocalDate.now());
      assertThat(entry.getEffectiveDate()).isEqualTo(effectiveDate);
    }
  }

  @Test
  void allEntries_haveNullReversalOfTransactionId() {
    UUID debtId = UUID.randomUUID();

    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(debtEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    service.recordPaymentReceived(debtId, new BigDecimal("500"), LocalDate.now(), "CREMUL-002");

    ArgumentCaptor<LedgerEntryEntity> captor = forClass(LedgerEntryEntity.class);
    verify(ledgerEntryRepository, times(2)).save(captor.capture());

    for (LedgerEntryEntity entry : captor.getAllValues()) {
      assertThat(entry.getReversalOfTransactionId()).isNull();
    }
  }
}
