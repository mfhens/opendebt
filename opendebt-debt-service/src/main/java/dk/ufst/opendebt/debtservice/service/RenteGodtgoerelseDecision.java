package dk.ufst.opendebt.debtservice.service;

import java.time.LocalDate;

public record RenteGodtgoerelseDecision(LocalDate startDate, ExceptionType exceptionApplied) {
  public enum ExceptionType {
    NONE,
    FIVE_BANKING_DAY,
    KILDESKATTELOV
  }
}
