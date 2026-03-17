package dk.ufst.opendebt.creditor.dto;

import lombok.*;

/** Result DTO for notification search containing the count of matching notifications. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSearchResultDto {

  private long matchingCount;
}
