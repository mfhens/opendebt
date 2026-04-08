package dk.ufst.opendebt.payment.bookkeeping.adapter;

import org.springframework.stereotype.Component;

import dk.ufst.bookkeeping.spi.BookkeepingAccountCode;
import dk.ufst.bookkeeping.spi.Kontoplan;
import dk.ufst.opendebt.payment.bookkeeping.AccountCode;

@Component
public class OpenDebtKontoplan implements Kontoplan {

  @Override
  public BookkeepingAccountCode receivables() {
    return AccountCode.RECEIVABLES;
  }

  @Override
  public BookkeepingAccountCode interestReceivable() {
    return AccountCode.INTEREST_RECEIVABLE;
  }

  @Override
  public BookkeepingAccountCode bank() {
    return AccountCode.SKB_BANK;
  }

  @Override
  public BookkeepingAccountCode collectionRevenue() {
    return AccountCode.COLLECTION_REVENUE;
  }

  @Override
  public BookkeepingAccountCode interestRevenue() {
    return AccountCode.INTEREST_REVENUE;
  }

  @Override
  public BookkeepingAccountCode writeOffExpense() {
    return AccountCode.WRITE_OFF_EXPENSE;
  }

  @Override
  public BookkeepingAccountCode offsettingClearing() {
    return AccountCode.OFFSETTING_CLEARING;
  }
}
