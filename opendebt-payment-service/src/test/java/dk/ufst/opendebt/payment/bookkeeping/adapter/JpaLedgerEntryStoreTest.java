package dk.ufst.opendebt.payment.bookkeeping.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import dk.ufst.bookkeeping.domain.EntryCategory;
import dk.ufst.bookkeeping.domain.EntryType;
import dk.ufst.bookkeeping.domain.LedgerEntry;
import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;
import dk.ufst.opendebt.payment.bookkeeping.repository.LedgerEntryRepository;
import dk.ufst.opendebt.payment.immudb.ImmuLedgerAppender;

@ExtendWith(MockitoExtension.class)
class JpaLedgerEntryStoreTest {

  @Mock private LedgerEntryRepository ledgerEntryRepository;
  @Mock private ImmuLedgerAppender immuLedgerAppender;

  @InjectMocks private JpaLedgerEntryStore store;

  // @InjectMocks uses constructor injection (only LedgerEntryRepository is
  // @RequiredArgsConstructor)
  // so immuLedgerAppender (an optional @Autowired field) must be set manually.
  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(store, "immuLedgerAppender", immuLedgerAppender);
  }

  // --- saveDoubleEntry ---

  @Test
  void saveDoubleEntry_savesBothEntriesAndAppendToImmudb() {
    LedgerEntry debit = buildEntry(EntryType.DEBIT, EntryCategory.PAYMENT);
    LedgerEntry credit = buildEntry(EntryType.CREDIT, EntryCategory.PAYMENT);

    LedgerEntryEntity savedDebit =
        LedgerEntryEntity.builder()
            .transactionId(debit.getTransactionId())
            .entryType(LedgerEntryEntity.EntryType.DEBIT)
            .build();
    LedgerEntryEntity savedCredit =
        LedgerEntryEntity.builder()
            .transactionId(credit.getTransactionId())
            .entryType(LedgerEntryEntity.EntryType.CREDIT)
            .build();

    when(ledgerEntryRepository.save(any())).thenReturn(savedDebit, savedCredit);

    store.saveDoubleEntry(debit, credit);

    verify(ledgerEntryRepository, times(2)).save(any(LedgerEntryEntity.class));
    verify(immuLedgerAppender).appendAsync(savedDebit, savedCredit);
  }

  @Test
  void saveDoubleEntry_withoutImmudb_doesNotThrow() {
    // immuLedgerAppender is null when not injected (optional bean)
    JpaLedgerEntryStore storeWithoutImmudb = new JpaLedgerEntryStore(ledgerEntryRepository);

    LedgerEntry debit = buildEntry(EntryType.DEBIT, EntryCategory.DEBT_REGISTRATION);
    LedgerEntry credit = buildEntry(EntryType.CREDIT, EntryCategory.DEBT_REGISTRATION);

    LedgerEntryEntity entity = LedgerEntryEntity.builder().build();
    when(ledgerEntryRepository.save(any())).thenReturn(entity);

    storeWithoutImmudb.saveDoubleEntry(debit, credit);

    verify(immuLedgerAppender, never()).appendAsync(any(), any());
  }

  // --- saveSingle ---

  @Test
  void saveSingle_savesOneEntry() {
    LedgerEntry entry = buildEntry(EntryType.DEBIT, EntryCategory.STORNO);
    when(ledgerEntryRepository.save(any())).thenReturn(LedgerEntryEntity.builder().build());

    store.saveSingle(entry);

    verify(ledgerEntryRepository).save(any(LedgerEntryEntity.class));
  }

  // --- finds ---

  @Test
  void findInterestAccrualsAfterDate_returnsMappedEntries() {
    UUID debtId = UUID.randomUUID();
    LocalDate from = LocalDate.of(2025, 1, 1);

    LedgerEntryEntity entity = buildEntity(LedgerEntryEntity.EntryType.DEBIT);
    when(ledgerEntryRepository.findInterestAccrualsAfterDate(debtId, from))
        .thenReturn(List.of(entity));

    List<LedgerEntry> result = store.findInterestAccrualsAfterDate(debtId, from);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getEntryType()).isEqualTo(EntryType.DEBIT);
  }

  @Test
  void findActiveEntriesByDebtId_returnsMappedEntries() {
    UUID debtId = UUID.randomUUID();
    LedgerEntryEntity entity = buildEntity(LedgerEntryEntity.EntryType.CREDIT);
    when(ledgerEntryRepository.findActiveEntriesByDebtId(debtId)).thenReturn(List.of(entity));

    List<LedgerEntry> result = store.findActiveEntriesByDebtId(debtId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getEntryType()).isEqualTo(EntryType.CREDIT);
  }

  @Test
  void findByTransactionId_returnsMappedEntries() {
    UUID txId = UUID.randomUUID();
    LedgerEntryEntity entity = buildEntity(LedgerEntryEntity.EntryType.DEBIT);
    when(ledgerEntryRepository.findByTransactionId(txId)).thenReturn(List.of(entity));

    List<LedgerEntry> result = store.findByTransactionId(txId);

    assertThat(result).hasSize(1);
  }

  @Test
  void existsByReversalOfTransactionId_delegatesToRepository() {
    UUID txId = UUID.randomUUID();
    when(ledgerEntryRepository.existsByReversalOfTransactionId(txId)).thenReturn(true);

    assertThat(store.existsByReversalOfTransactionId(txId)).isTrue();
  }

  // --- enum mapping ---

  @Test
  void toEntity_mapsAllEntryCategories() {
    for (EntryCategory category : EntryCategory.values()) {
      LedgerEntry entry = buildEntry(EntryType.DEBIT, category);
      when(ledgerEntryRepository.save(any())).thenReturn(LedgerEntryEntity.builder().build());
      store.saveSingle(entry);
    }
    // No exception = all cases handled in switch
  }

  @Test
  void fromEntity_mapsAllEntryCategoriesBack() {
    UUID debtId = UUID.randomUUID();
    for (LedgerEntryEntity.EntryCategory category : LedgerEntryEntity.EntryCategory.values()) {
      LedgerEntryEntity entity =
          LedgerEntryEntity.builder()
              .entryType(LedgerEntryEntity.EntryType.DEBIT)
              .entryCategory(category)
              .build();
      when(ledgerEntryRepository.findActiveEntriesByDebtId(debtId)).thenReturn(List.of(entity));
      store.findActiveEntriesByDebtId(debtId);
    }
  }

  // --- fieldmapping round-trip ---

  @Test
  void toEntity_mapsAllFieldsCorrectly() {
    UUID txId = UUID.randomUUID();
    UUID debtId = UUID.randomUUID();
    UUID reversalId = UUID.randomUUID();

    LedgerEntry entry =
        LedgerEntry.builder()
            .transactionId(txId)
            .debtId(debtId)
            .accountCode("1000")
            .accountName("Fordringer")
            .entryType(EntryType.DEBIT)
            .amount(new BigDecimal("500.00"))
            .effectiveDate(LocalDate.of(2025, 6, 1))
            .postingDate(LocalDate.of(2025, 6, 2))
            .reference("REF-001")
            .description("Test")
            .reversalOfTransactionId(reversalId)
            .entryCategory(EntryCategory.PAYMENT)
            .build();

    ArgumentCaptor<LedgerEntryEntity> captor = ArgumentCaptor.forClass(LedgerEntryEntity.class);
    when(ledgerEntryRepository.save(any())).thenReturn(LedgerEntryEntity.builder().build());

    store.saveSingle(entry);

    verify(ledgerEntryRepository).save(captor.capture());
    LedgerEntryEntity saved = captor.getValue();

    assertThat(saved.getTransactionId()).isEqualTo(txId);
    assertThat(saved.getDebtId()).isEqualTo(debtId);
    assertThat(saved.getAccountCode()).isEqualTo("1000");
    assertThat(saved.getAccountName()).isEqualTo("Fordringer");
    assertThat(saved.getEntryType()).isEqualTo(LedgerEntryEntity.EntryType.DEBIT);
    assertThat(saved.getAmount()).isEqualByComparingTo("500.00");
    assertThat(saved.getEffectiveDate()).isEqualTo(LocalDate.of(2025, 6, 1));
    assertThat(saved.getPostingDate()).isEqualTo(LocalDate.of(2025, 6, 2));
    assertThat(saved.getReference()).isEqualTo("REF-001");
    assertThat(saved.getDescription()).isEqualTo("Test");
    assertThat(saved.getReversalOfTransactionId()).isEqualTo(reversalId);
    assertThat(saved.getEntryCategory()).isEqualTo(LedgerEntryEntity.EntryCategory.PAYMENT);
  }

  // --- helpers ---

  private LedgerEntry buildEntry(EntryType entryType, EntryCategory category) {
    return LedgerEntry.builder()
        .transactionId(UUID.randomUUID())
        .debtId(UUID.randomUUID())
        .accountCode("1000")
        .accountName("Fordringer")
        .entryType(entryType)
        .amount(new BigDecimal("100.00"))
        .effectiveDate(LocalDate.now())
        .postingDate(LocalDate.now())
        .reference("REF")
        .description("desc")
        .entryCategory(category)
        .build();
  }

  private LedgerEntryEntity buildEntity(LedgerEntryEntity.EntryType entryType) {
    return LedgerEntryEntity.builder()
        .transactionId(UUID.randomUUID())
        .debtId(UUID.randomUUID())
        .accountCode("1000")
        .accountName("Fordringer")
        .entryType(entryType)
        .amount(new BigDecimal("100.00"))
        .effectiveDate(LocalDate.now())
        .postingDate(LocalDate.now())
        .reference("REF")
        .description("desc")
        .entryCategory(LedgerEntryEntity.EntryCategory.PAYMENT)
        .build();
  }
}
