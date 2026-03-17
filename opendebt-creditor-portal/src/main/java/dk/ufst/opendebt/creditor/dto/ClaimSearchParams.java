package dk.ufst.opendebt.creditor.dto;

import java.time.LocalDate;

import lombok.*;

/** Search and filter parameters for claim list queries. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimSearchParams {

  private String searchQuery;
  private String searchType;
  private LocalDate dateFrom;
  private LocalDate dateTo;
}
