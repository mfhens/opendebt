package dk.ufst.opendebt.creditor.dto;

import java.util.UUID;

import lombok.*;

/** DTO representing detailed reconciliation information for the detail view. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationDetailDto {

  private UUID id;
  private String status;
  private int year;
  private int month;
  private ReconciliationBasisDto basis;
  private ReconciliationResponseDto previousResponse;
}
