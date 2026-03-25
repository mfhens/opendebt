package dk.ufst.opendebt.common.dto.soap;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCollectionResult {
  private String claimId;

  @JsonProperty("underretninger")
  private List<NotificationDto> notifications;

  private int total;
}
