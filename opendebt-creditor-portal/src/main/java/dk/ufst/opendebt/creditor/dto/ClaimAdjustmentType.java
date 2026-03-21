package dk.ufst.opendebt.creditor.dto;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Enum representing the eight supported claim adjustment (write-up / write-down) types defined by
 * petition 034. Each type carries its direction (WRITE_UP or WRITE_DOWN), the creditor agreement
 * permission flag it requires, and whether it is payment-related (requiring debtor selection for
 * multi-debtor claims).
 */
public enum ClaimAdjustmentType {

  // --- Write-downs (nedskrivninger) ---
  NEDSKRIV(Direction.WRITE_DOWN, "allowWriteDown", false),
  NEDSKRIVNING_INDBETALING(Direction.WRITE_DOWN, "allowWriteDownPayment", true),
  NEDSKRIVNING_ANNULLERET_OPSKRIVNING_REGULERING(Direction.WRITE_DOWN, "allowWriteDown", false),
  NEDSKRIVNING_ANNULLERET_OPSKRIVNING_INDBETALING(
      Direction.WRITE_DOWN, "allowWriteDownPayment", true),

  // --- Write-ups (opskrivninger) ---
  OPSKRIVNING_REGULERING(Direction.WRITE_UP, "allowWriteUpAdjustment", false),
  OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING(Direction.WRITE_UP, "allowWriteUpAdjustment", false),
  OPSKRIVNING_ANNULLERET_NEDSKRIVNING_INDBETALING(Direction.WRITE_UP, "allowWriteUpPayment", true),
  FEJLAGTIG_HOVEDSTOL_INDBERETNING(Direction.WRITE_UP, "allowPrincipalCorrection", false);

  public enum Direction {
    WRITE_UP,
    WRITE_DOWN
  }

  private final Direction direction;
  private final String requiredPermission;
  private final boolean paymentRelated;

  ClaimAdjustmentType(Direction direction, String requiredPermission, boolean paymentRelated) {
    this.direction = direction;
    this.requiredPermission = requiredPermission;
    this.paymentRelated = paymentRelated;
  }

  public Direction getDirection() {
    return direction;
  }

  public String getRequiredPermission() {
    return requiredPermission;
  }

  public boolean isPaymentRelated() {
    return paymentRelated;
  }

  /** Returns all types for a given direction. */
  public static List<ClaimAdjustmentType> forDirection(Direction direction) {
    return Arrays.stream(values()).filter(t -> t.direction == direction).toList();
  }

  /**
   * Filters adjustment types by a set of granted permission flags from the creditor agreement.
   *
   * @param direction the direction (WRITE_UP or WRITE_DOWN)
   * @param grantedPermissions the permission flags granted by the creditor agreement
   * @return the list of adjustment types the creditor is permitted to use
   */
  public static List<ClaimAdjustmentType> filterByPermissions(
      Direction direction, Set<String> grantedPermissions) {
    return forDirection(direction).stream()
        .filter(t -> grantedPermissions.contains(t.requiredPermission))
        .toList();
  }
}
