package dk.ufst.opendebt.payment.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.payment.bookkeeping.AccountCode;
import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;
import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity.EventType;
import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;
import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity.EntryCategory;
import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity.EntryType;
import dk.ufst.opendebt.payment.bookkeeping.repository.DebtEventRepository;
import dk.ufst.opendebt.payment.bookkeeping.repository.LedgerEntryRepository;
import dk.ufst.opendebt.payment.immudb.ImmuLedgerAppender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds demo crossing-transaction data for the caseworker portal. Activated for all local dev
 * profiles (dev, local, demo, demo-auth). Seeding is idempotent — if data already exists for Debt A
 * it is skipped.
 */
@Slf4j
@Component
@Profile("dev | local | demo | demo-auth")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

  private final LedgerEntryRepository ledgerEntryRepository;
  private final DebtEventRepository debtEventRepository;
  private final ImmuLedgerAppender immuLedgerAppender;

  // Fixed UUIDs for deterministic, idempotent demo data
  private static final UUID DEBT_A_ID = UUID.fromString("00000000-0000-0000-0000-000000000A01");
  private static final UUID DEBT_B_ID = UUID.fromString("00000000-0000-0000-0000-000000000B01");

  private static final String AMOUNT_375_00 = "375.00";
  private static final String AMOUNT_453_75 = "453.75";
  private static final String AMOUNT_5000_00 = "5000.00";

  // Debt A transaction IDs
  private static final UUID TXN_A01 = UUID.fromString("A0000000-0000-0000-0000-000000000001");
  private static final UUID TXN_A02 = UUID.fromString("A0000000-0000-0000-0000-000000000002");
  private static final UUID TXN_A03 = UUID.fromString("A0000000-0000-0000-0000-000000000003");
  // Crossing-transaction group: entries 4-9 share the same txn ID
  private static final UUID TXN_A04_CROSSING =
      UUID.fromString("A0000000-0000-0000-0000-000000000004");
  private static final UUID TXN_A10 = UUID.fromString("A0000000-0000-0000-0000-000000000010");
  private static final UUID TXN_A11 = UUID.fromString("A0000000-0000-0000-0000-000000000011");

  // Debt B transaction IDs
  private static final UUID TXN_B01 = UUID.fromString("B0000000-0000-0000-0000-000000000001");
  private static final UUID TXN_B02 = UUID.fromString("B0000000-0000-0000-0000-000000000002");
  private static final UUID TXN_B03 = UUID.fromString("B0000000-0000-0000-0000-000000000003");

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!debtEventRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_A_ID).isEmpty()) {
      log.info("Demo data already seeded, skipping.");
      return;
    }
    seedDebtA();
    seedDebtB();
    log.info("Demo crossing-transaction data seeded for debts A={} and B={}", DEBT_A_ID, DEBT_B_ID);
  }

  // ────────────────────────────────────────────────────────────────────────
  // Debt A — Tax debt, 45,000 DKK with crossing transaction
  // ────────────────────────────────────────────────────────────────────────
  private void seedDebtA() {
    // #1 DEBT_REGISTERED 2025-02-01 45,000
    saveEvent(
        DEBT_A_ID,
        EventType.DEBT_REGISTERED,
        LocalDate.of(2025, 2, 1),
        new BigDecimal("45000.00"),
        TXN_A01,
        "Initial registration of tax debt");
    saveLedgerPair(
        TXN_A01,
        DEBT_A_ID,
        AccountCode.RECEIVABLES,
        AccountCode.COLLECTION_REVENUE,
        new BigDecimal("45000.00"),
        LocalDate.of(2025, 2, 1),
        LocalDate.of(2025, 2, 1),
        EntryCategory.DEBT_REGISTRATION,
        null,
        "Initial registration of tax debt");

    // #2 INTEREST_ACCRUED 2025-03-01 375.00
    saveEvent(
        DEBT_A_ID,
        EventType.INTEREST_ACCRUED,
        LocalDate.of(2025, 3, 1),
        new BigDecimal(AMOUNT_375_00),
        TXN_A02,
        "Monthly interest Feb");
    saveLedgerPair(
        TXN_A02,
        DEBT_A_ID,
        AccountCode.INTEREST_RECEIVABLE,
        AccountCode.INTEREST_REVENUE,
        new BigDecimal(AMOUNT_375_00),
        LocalDate.of(2025, 3, 1),
        LocalDate.of(2025, 3, 1),
        EntryCategory.INTEREST_ACCRUAL,
        null,
        "Monthly interest Feb");

    // #3 INTEREST_ACCRUED 2025-04-01 453.75
    saveEvent(
        DEBT_A_ID,
        EventType.INTEREST_ACCRUED,
        LocalDate.of(2025, 4, 1),
        new BigDecimal(AMOUNT_453_75),
        TXN_A03,
        "Monthly interest Mar");
    saveLedgerPair(
        TXN_A03,
        DEBT_A_ID,
        AccountCode.INTEREST_RECEIVABLE,
        AccountCode.INTEREST_REVENUE,
        new BigDecimal(AMOUNT_453_75),
        LocalDate.of(2025, 4, 1),
        LocalDate.of(2025, 4, 1),
        EntryCategory.INTEREST_ACCRUAL,
        null,
        "Monthly interest Mar");

    // --- Crossing-transaction group (entries 4-9 share TXN_A04_CROSSING) ---

    // #4 PAYMENT_RECEIVED effective 2025-02-15, posted 2025-04-10 10,000
    saveEvent(
        DEBT_A_ID,
        EventType.PAYMENT_RECEIVED,
        LocalDate.of(2025, 2, 15),
        new BigDecimal("10000.00"),
        TXN_A04_CROSSING,
        "Crossing payment (vaerdidag before interest)");
    saveLedgerPair(
        TXN_A04_CROSSING,
        DEBT_A_ID,
        AccountCode.SKB_BANK,
        AccountCode.RECEIVABLES,
        new BigDecimal("10000.00"),
        LocalDate.of(2025, 2, 15),
        LocalDate.of(2025, 4, 10),
        EntryCategory.PAYMENT,
        null,
        "Crossing payment (vaerdidag before interest)");

    // #5 Storno of #2 (Feb interest reversal)
    saveEvent(
        DEBT_A_ID,
        EventType.INTEREST_ACCRUED,
        LocalDate.of(2025, 3, 1),
        new BigDecimal(AMOUNT_375_00),
        TXN_A04_CROSSING,
        "Reversal of Feb interest");
    saveLedgerPair(
        TXN_A04_CROSSING,
        DEBT_A_ID,
        AccountCode.INTEREST_REVENUE,
        AccountCode.INTEREST_RECEIVABLE,
        new BigDecimal(AMOUNT_375_00),
        LocalDate.of(2025, 3, 1),
        LocalDate.of(2025, 4, 10),
        EntryCategory.STORNO,
        TXN_A02,
        "Reversal of Feb interest");

    // #6 Storno of #3 (Mar interest reversal)
    saveEvent(
        DEBT_A_ID,
        EventType.INTEREST_ACCRUED,
        LocalDate.of(2025, 4, 1),
        new BigDecimal(AMOUNT_453_75),
        TXN_A04_CROSSING,
        "Reversal of Mar interest");
    saveLedgerPair(
        TXN_A04_CROSSING,
        DEBT_A_ID,
        AccountCode.INTEREST_REVENUE,
        AccountCode.INTEREST_RECEIVABLE,
        new BigDecimal(AMOUNT_453_75),
        LocalDate.of(2025, 4, 1),
        LocalDate.of(2025, 4, 10),
        EntryCategory.STORNO,
        TXN_A03,
        "Reversal of Mar interest");

    // #7 INTEREST_ACCRUED 2025-03-01 recalculated on 35,000 = 291.67
    saveEvent(
        DEBT_A_ID,
        EventType.INTEREST_ACCRUED,
        LocalDate.of(2025, 3, 1),
        new BigDecimal("291.67"),
        TXN_A04_CROSSING,
        "Recalculated Feb interest on 35,000");
    saveLedgerPair(
        TXN_A04_CROSSING,
        DEBT_A_ID,
        AccountCode.INTEREST_RECEIVABLE,
        AccountCode.INTEREST_REVENUE,
        new BigDecimal("291.67"),
        LocalDate.of(2025, 3, 1),
        LocalDate.of(2025, 4, 10),
        EntryCategory.INTEREST_ACCRUAL,
        null,
        "Recalculated Feb interest on 35,000");

    // #8 INTEREST_ACCRUED 2025-04-01 recalculated = 352.92
    saveEvent(
        DEBT_A_ID,
        EventType.INTEREST_ACCRUED,
        LocalDate.of(2025, 4, 1),
        new BigDecimal("352.92"),
        TXN_A04_CROSSING,
        "Recalculated Mar interest");
    saveLedgerPair(
        TXN_A04_CROSSING,
        DEBT_A_ID,
        AccountCode.INTEREST_RECEIVABLE,
        AccountCode.INTEREST_REVENUE,
        new BigDecimal("352.92"),
        LocalDate.of(2025, 4, 1),
        LocalDate.of(2025, 4, 10),
        EntryCategory.INTEREST_ACCRUAL,
        null,
        "Recalculated Mar interest");

    // #9 COVERAGE_REVERSED marker (zero amount)
    saveEvent(
        DEBT_A_ID,
        EventType.COVERAGE_REVERSED,
        LocalDate.of(2025, 2, 15),
        BigDecimal.ZERO,
        TXN_A04_CROSSING,
        "Daekningsophaevelse marker");
    saveLedgerPairZero(
        TXN_A04_CROSSING,
        DEBT_A_ID,
        LocalDate.of(2025, 2, 15),
        LocalDate.of(2025, 4, 10),
        EntryCategory.COVERAGE_REVERSAL,
        "Daekningsophaevelse marker");

    // --- End crossing-transaction group ---

    // #10 PAYMENT_RECEIVED 2025-05-01 5,000
    saveEvent(
        DEBT_A_ID,
        EventType.PAYMENT_RECEIVED,
        LocalDate.of(2025, 5, 1),
        new BigDecimal(AMOUNT_5000_00),
        TXN_A10,
        "Normal payment");
    saveLedgerPair(
        TXN_A10,
        DEBT_A_ID,
        AccountCode.SKB_BANK,
        AccountCode.RECEIVABLES,
        new BigDecimal(AMOUNT_5000_00),
        LocalDate.of(2025, 5, 1),
        LocalDate.of(2025, 5, 1),
        EntryCategory.PAYMENT,
        null,
        "Normal payment");

    // #11 INTEREST_ACCRUED 2025-05-01 306.44
    saveEvent(
        DEBT_A_ID,
        EventType.INTEREST_ACCRUED,
        LocalDate.of(2025, 5, 1),
        new BigDecimal("306.44"),
        TXN_A11,
        "Interest after normal payment");
    saveLedgerPair(
        TXN_A11,
        DEBT_A_ID,
        AccountCode.INTEREST_RECEIVABLE,
        AccountCode.INTEREST_REVENUE,
        new BigDecimal("306.44"),
        LocalDate.of(2025, 5, 1),
        LocalDate.of(2025, 5, 1),
        EntryCategory.INTEREST_ACCRUAL,
        null,
        "Interest after normal payment");
  }

  // ────────────────────────────────────────────────────────────────────────
  // Debt B — Fine debt, 12,500 DKK (simple timeline, no crossing)
  // ────────────────────────────────────────────────────────────────────────
  private void seedDebtB() {
    // #1 DEBT_REGISTERED 2025-03-15 12,500
    saveEvent(
        DEBT_B_ID,
        EventType.DEBT_REGISTERED,
        LocalDate.of(2025, 3, 15),
        new BigDecimal("12500.00"),
        TXN_B01,
        "Initial registration of fine debt");
    saveLedgerPair(
        TXN_B01,
        DEBT_B_ID,
        AccountCode.RECEIVABLES,
        AccountCode.COLLECTION_REVENUE,
        new BigDecimal("12500.00"),
        LocalDate.of(2025, 3, 15),
        LocalDate.of(2025, 3, 15),
        EntryCategory.DEBT_REGISTRATION,
        null,
        "Initial registration of fine debt");

    // #2 PAYMENT_RECEIVED 2025-04-15 5,000
    saveEvent(
        DEBT_B_ID,
        EventType.PAYMENT_RECEIVED,
        LocalDate.of(2025, 4, 15),
        new BigDecimal(AMOUNT_5000_00),
        TXN_B02,
        "Payment received");
    saveLedgerPair(
        TXN_B02,
        DEBT_B_ID,
        AccountCode.SKB_BANK,
        AccountCode.RECEIVABLES,
        new BigDecimal(AMOUNT_5000_00),
        LocalDate.of(2025, 4, 15),
        LocalDate.of(2025, 4, 15),
        EntryCategory.PAYMENT,
        null,
        "Payment received");

    // #3 INTEREST_ACCRUED 2025-05-01 75.00
    saveEvent(
        DEBT_B_ID,
        EventType.INTEREST_ACCRUED,
        LocalDate.of(2025, 5, 1),
        new BigDecimal("75.00"),
        TXN_B03,
        "Monthly interest");
    saveLedgerPair(
        TXN_B03,
        DEBT_B_ID,
        AccountCode.INTEREST_RECEIVABLE,
        AccountCode.INTEREST_REVENUE,
        new BigDecimal("75.00"),
        LocalDate.of(2025, 5, 1),
        LocalDate.of(2025, 5, 1),
        EntryCategory.INTEREST_ACCRUAL,
        null,
        "Monthly interest");
  }

  // ────────────────────────────────────────────────────────────────────────
  // Helper methods
  // ────────────────────────────────────────────────────────────────────────

  private void saveEvent(
      UUID debtId,
      EventType eventType,
      LocalDate effectiveDate,
      BigDecimal amount,
      UUID ledgerTransactionId,
      String description) {
    DebtEventEntity event =
        DebtEventEntity.builder()
            .debtId(debtId)
            .eventType(eventType)
            .effectiveDate(effectiveDate)
            .amount(amount)
            .ledgerTransactionId(ledgerTransactionId)
            .description(description)
            .build();
    debtEventRepository.save(event);
  }

  private void saveLedgerPair(
      UUID transactionId,
      UUID debtId,
      AccountCode debitAccount,
      AccountCode creditAccount,
      BigDecimal amount,
      LocalDate effectiveDate,
      LocalDate postingDate,
      EntryCategory category,
      UUID reversalOfTransactionId,
      String description) {
    LedgerEntryEntity debit =
        LedgerEntryEntity.builder()
            .transactionId(transactionId)
            .debtId(debtId)
            .accountCode(debitAccount.getCode())
            .accountName(debitAccount.getName())
            .entryType(EntryType.DEBIT)
            .amount(amount)
            .effectiveDate(effectiveDate)
            .postingDate(postingDate)
            .entryCategory(category)
            .reversalOfTransactionId(reversalOfTransactionId)
            .description(description)
            .build();
    LedgerEntryEntity savedDebit = ledgerEntryRepository.save(debit);

    LedgerEntryEntity credit =
        LedgerEntryEntity.builder()
            .transactionId(transactionId)
            .debtId(debtId)
            .accountCode(creditAccount.getCode())
            .accountName(creditAccount.getName())
            .entryType(EntryType.CREDIT)
            .amount(amount)
            .effectiveDate(effectiveDate)
            .postingDate(postingDate)
            .entryCategory(category)
            .reversalOfTransactionId(reversalOfTransactionId)
            .description(description)
            .build();
    LedgerEntryEntity savedCredit = ledgerEntryRepository.save(credit);
    immuLedgerAppender.appendAsync(savedDebit, savedCredit);
  }

  /**
   * Creates a zero-amount ledger pair for marker entries (e.g., coverage reversal). Uses
   * RECEIVABLES for both debit and credit since the amount is zero.
   */
  private void saveLedgerPairZero(
      UUID transactionId,
      UUID debtId,
      LocalDate effectiveDate,
      LocalDate postingDate,
      EntryCategory category,
      String description) {
    LedgerEntryEntity debit =
        LedgerEntryEntity.builder()
            .transactionId(transactionId)
            .debtId(debtId)
            .accountCode(AccountCode.RECEIVABLES.getCode())
            .accountName(AccountCode.RECEIVABLES.getName())
            .entryType(EntryType.DEBIT)
            .amount(BigDecimal.ZERO)
            .effectiveDate(effectiveDate)
            .postingDate(postingDate)
            .entryCategory(category)
            .description(description)
            .build();
    LedgerEntryEntity savedDebit = ledgerEntryRepository.save(debit);

    LedgerEntryEntity credit =
        LedgerEntryEntity.builder()
            .transactionId(transactionId)
            .debtId(debtId)
            .accountCode(AccountCode.RECEIVABLES.getCode())
            .accountName(AccountCode.RECEIVABLES.getName())
            .entryType(EntryType.CREDIT)
            .amount(BigDecimal.ZERO)
            .effectiveDate(effectiveDate)
            .postingDate(postingDate)
            .entryCategory(category)
            .description(description)
            .build();
    LedgerEntryEntity savedCredit = ledgerEntryRepository.save(credit);
    immuLedgerAppender.appendAsync(savedDebit, savedCredit);
  }
}
