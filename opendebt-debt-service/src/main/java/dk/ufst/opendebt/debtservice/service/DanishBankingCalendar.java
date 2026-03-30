package dk.ufst.opendebt.debtservice.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

@Component
public class DanishBankingCalendar {

  public int bankingDaysBetween(LocalDate from, LocalDate to) {
    int count = 0;
    LocalDate current = from.plusDays(1);
    while (!current.isAfter(to)) {
      DayOfWeek day = current.getDayOfWeek();
      if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
        count++;
      }
      current = current.plusDays(1);
    }
    return count;
  }
}
