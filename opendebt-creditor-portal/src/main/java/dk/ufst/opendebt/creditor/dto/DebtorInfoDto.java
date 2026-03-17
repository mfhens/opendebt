package dk.ufst.opendebt.creditor.dto;

import lombok.*;

/**
 * DTO representing a debtor associated with a claim via the haeftelsesstruktur (liability
 * structure). The identifier type indicates CPR, CVR, SE, or AKR.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebtorInfoDto {

  private String identifierType;
  private String identifier;
}
