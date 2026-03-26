package dk.ufst.opendebt.gateway.soap;

import java.time.LocalDate;

import dk.ufst.opendebt.common.dto.soap.ClaimSubmissionResponse;

public final class ClaimMapperUtils {

  private ClaimMapperUtils() {}

  public static String mapOutcome(ClaimSubmissionResponse.Outcome outcome) {
    if (outcome == null) return "FEJL";
    return switch (outcome) {
      case SUCCESS -> "MODTAGET";
      case REJECTED -> "AFVIST";
      case ERROR -> "FEJL";
    };
  }

  public static LocalDate parseDate(String s) {
    if (s == null || s.isBlank()) return null;
    try {
      return LocalDate.parse(s.trim());
    } catch (Exception e) {
      return null;
    }
  }
}
