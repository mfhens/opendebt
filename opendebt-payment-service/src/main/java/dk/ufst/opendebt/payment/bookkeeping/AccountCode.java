package dk.ufst.opendebt.payment.bookkeeping;

import dk.ufst.bookkeeping.spi.AccountType;
import dk.ufst.bookkeeping.spi.BookkeepingAccountCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Chart of accounts (kontoplan) for OpenDebt bookkeeping, aligned with statsligt regnskab
 * conventions. See ADR-0018 for design rationale.
 */
@Getter
@RequiredArgsConstructor
public enum AccountCode implements BookkeepingAccountCode {
  RECEIVABLES("1000", "Fordringer", AccountType.ASSET),
  INTEREST_RECEIVABLE("1100", "Renter tilgodehavende", AccountType.ASSET),
  SKB_BANK("2000", "SKB Bankkonto", AccountType.ASSET),
  COLLECTION_REVENUE("3000", "Indrivelsesindtaegter", AccountType.REVENUE),
  INTEREST_REVENUE("3100", "Renteindtaegter", AccountType.REVENUE),
  WRITE_OFF_EXPENSE("4000", "Tab paa fordringer", AccountType.EXPENSE),
  OFFSETTING_CLEARING("5000", "Modregning clearing", AccountType.LIABILITY);

  private final String code;
  private final String name;
  private final AccountType type;
}
