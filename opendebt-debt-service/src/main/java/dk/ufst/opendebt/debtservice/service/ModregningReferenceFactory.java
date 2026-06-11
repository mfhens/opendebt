package dk.ufst.opendebt.debtservice.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class ModregningReferenceFactory {

  private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

  private ModregningReferenceFactory() {}

  public static String decisionReference(String nemkontoReferenceId) {
    return "DEC-" + nemkontoReferenceId;
  }

  public static String lineageReference(String nemkontoReferenceId) {
    return "LIN-" + nemkontoReferenceId;
  }

  public static String settlementSuffix(LocalDate settlementDate, UUID entryId) {
    return settlementDate.format(BASIC_DATE) + "-" + entryId;
  }
}
