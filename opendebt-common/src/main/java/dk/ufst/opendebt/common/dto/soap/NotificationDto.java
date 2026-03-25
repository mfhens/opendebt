package dk.ufst.opendebt.common.dto.soap;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
  private String notificationId;
  private String claimId;
  private String type;
  private String status;
  private Instant createdAt;
  private String description;
}
