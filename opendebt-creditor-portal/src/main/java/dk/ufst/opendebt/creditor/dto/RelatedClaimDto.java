package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.*;

/** DTO representing a related claim (underfordring) shown in the claim detail view. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatedClaimDto {

  private UUID claimId;
  private String claimType;
  private String claimCategory;
  private BigDecimal balance;
  private String status;
}
