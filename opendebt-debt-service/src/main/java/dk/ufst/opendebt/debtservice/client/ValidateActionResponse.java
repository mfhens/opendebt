package dk.ufst.opendebt.debtservice.client;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateActionResponse {

  private boolean allowed;
  private CreditorAction requestedAction;
  private String reasonCode;
  private String message;
}
