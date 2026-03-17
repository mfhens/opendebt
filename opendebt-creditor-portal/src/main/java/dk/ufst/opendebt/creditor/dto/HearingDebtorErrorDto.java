package dk.ufst.opendebt.creditor.dto;

import java.util.List;

import lombok.*;

/**
 * DTO representing a debtor in a hearing claim's skyldnerliste with associated error types
 * (fejltyper).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HearingDebtorErrorDto {

  private String debtorType;
  private String debtorIdentifier;
  private List<String> errorTypes;
}
