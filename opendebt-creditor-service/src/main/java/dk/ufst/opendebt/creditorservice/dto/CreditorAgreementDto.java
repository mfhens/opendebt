package dk.ufst.opendebt.creditorservice.dto;

import java.util.List;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditorAgreementDto {

  private boolean portalActionsAllowed;
  private boolean allowCreateRecoveryClaims;
  private List<String> allowedClaimTypes;
  private List<String> allowedDebtorTypes;
  private List<String> allowedInterestRules;
  private String notificationPreference;
  private List<String> enabledNotificationTypes;
  private String contactEmail;
  private boolean allowWriteDown;
  private boolean allowWriteDownPayment;
  private boolean allowWriteUpAdjustment;
  private boolean allowWriteUpPayment;
  private boolean allowPrincipalCorrection;
}
