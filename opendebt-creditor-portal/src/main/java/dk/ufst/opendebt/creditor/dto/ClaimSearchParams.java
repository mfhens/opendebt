package dk.ufst.opendebt.creditor.dto;

import java.time.LocalDate;

import lombok.*;

/** Pagination, sorting, search, and date-range parameters for claim list queries. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimSearchParams {

  @Builder.Default private int page = 0;

  @Builder.Default private int size = 20;

  private String sortBy;

  @Builder.Default private String sortDirection = "asc";

  private String searchQuery;
  private String searchType;
  private LocalDate dateFrom;
  private LocalDate dateTo;
}
