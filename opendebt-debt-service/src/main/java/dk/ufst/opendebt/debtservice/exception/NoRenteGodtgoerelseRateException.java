package dk.ufst.opendebt.debtservice.exception;

import java.time.LocalDate;

public class NoRenteGodtgoerelseRateException extends RuntimeException {
  public NoRenteGodtgoerelseRateException(LocalDate date) {
    super("No RenteGodtgoerelseRateEntry found for date: " + date);
  }
}
