package dk.ufst.opendebt.payment.bookkeeping.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.payment.bookkeeping.AccountCode;

class OpenDebtKontoplanTest {

  private final OpenDebtKontoplan kontoplan = new OpenDebtKontoplan();

  @Test
  void receivables_returnsCorrectAccountCode() {
    assertThat(kontoplan.receivables()).isEqualTo(AccountCode.RECEIVABLES);
  }

  @Test
  void interestReceivable_returnsCorrectAccountCode() {
    assertThat(kontoplan.interestReceivable()).isEqualTo(AccountCode.INTEREST_RECEIVABLE);
  }

  @Test
  void bank_returnsCorrectAccountCode() {
    assertThat(kontoplan.bank()).isEqualTo(AccountCode.SKB_BANK);
  }

  @Test
  void collectionRevenue_returnsCorrectAccountCode() {
    assertThat(kontoplan.collectionRevenue()).isEqualTo(AccountCode.COLLECTION_REVENUE);
  }

  @Test
  void interestRevenue_returnsCorrectAccountCode() {
    assertThat(kontoplan.interestRevenue()).isEqualTo(AccountCode.INTEREST_REVENUE);
  }

  @Test
  void writeOffExpense_returnsCorrectAccountCode() {
    assertThat(kontoplan.writeOffExpense()).isEqualTo(AccountCode.WRITE_OFF_EXPENSE);
  }

  @Test
  void offsettingClearing_returnsCorrectAccountCode() {
    assertThat(kontoplan.offsettingClearing()).isEqualTo(AccountCode.OFFSETTING_CLEARING);
  }
}
