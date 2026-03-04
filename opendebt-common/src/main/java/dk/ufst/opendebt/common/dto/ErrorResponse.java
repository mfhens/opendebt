package dk.ufst.opendebt.common.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {

  private LocalDateTime timestamp;
  private String errorCode;
  private String message;
  private String severity;
  private List<String> details;
  private String traceId;
}
