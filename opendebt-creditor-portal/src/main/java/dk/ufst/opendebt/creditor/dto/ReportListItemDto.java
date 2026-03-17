package dk.ufst.opendebt.creditor.dto;

import java.util.UUID;

import lombok.*;

/** DTO representing a single report entry in the monthly report list (petition 037). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportListItemDto {

  private UUID reportId;
  private String reportName;
  private String reportType;
  private String availabilityStatus;
  private boolean reconciliationSummary;
}
