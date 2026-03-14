package dk.ufst.opendebt.creditorservice.dto;

import dk.ufst.opendebt.creditorservice.action.CreditorAction;

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
