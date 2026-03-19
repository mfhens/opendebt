package dk.ufst.opendebt.gateway.creditor.dto;

import java.time.Instant;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayErrorResponse {

  private String errorCode;
  private String message;
  private String correlationId;
  private Instant timestamp;
}
