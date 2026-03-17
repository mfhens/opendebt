package dk.ufst.opendebt.creditor.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.*;

/** Form DTO for notification search parameters. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSearchDto {

  private LocalDate dateFrom;
  private LocalDate dateTo;
  private List<String> notificationTypes;
  private boolean formatPdf;
  private boolean formatXml;
}
