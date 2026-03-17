package dk.ufst.opendebt.creditor.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.*;

/** Creditor agreement configuration returned by creditor-service. */
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

  // --- Write-up / write-down permission flags (petition 034) ---
  private boolean allowWriteDown;
  private boolean allowWriteDownPayment;
  private boolean allowWriteUpAdjustment;
  private boolean allowWriteUpPayment;
  private boolean allowPrincipalCorrection;

  /**
   * Returns the set of granted adjustment permission flag names, suitable for filtering adjustment
   * types with {@link ClaimAdjustmentType#filterByPermissions}.
   */
  public Set<String> getGrantedAdjustmentPermissions() {
    Set<String> permissions = new HashSet<>();
    if (allowWriteDown) {
      permissions.add("allowWriteDown");
    }
    if (allowWriteDownPayment) {
      permissions.add("allowWriteDownPayment");
    }
    if (allowWriteUpAdjustment) {
      permissions.add("allowWriteUpAdjustment");
    }
    if (allowWriteUpPayment) {
      permissions.add("allowWriteUpPayment");
    }
    if (allowPrincipalCorrection) {
      permissions.add("allowPrincipalCorrection");
    }
    return permissions;
  }
}
