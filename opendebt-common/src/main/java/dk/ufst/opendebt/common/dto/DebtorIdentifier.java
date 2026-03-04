package dk.ufst.opendebt.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import lombok.Builder;
import lombok.Data;

/**
 * Identifies a debtor in the OpenDebt system.
 *
 * <p>A debtor can be:
 *
 * <ul>
 *   <li>A natural person identified by CPR (personnummer)
 *   <li>A company identified by CVR
 *   <li>A natural person acting as a business (identified by CPR, role BUSINESS)
 * </ul>
 *
 * <p>Important: A natural person (CPR) can have debt in two roles:
 *
 * <ul>
 *   <li>PERSONAL - debt as a private individual
 *   <li>BUSINESS - debt as a sole proprietor (enkeltmandsvirksomhed)
 * </ul>
 */
@Data
@Builder
public class DebtorIdentifier {

  @NotBlank(message = "Identifier is required")
  @Pattern(regexp = "^[0-9]{8,10}$", message = "Must be valid CPR (10 digits) or CVR (8 digits)")
  private String identifier;

  @NotNull(message = "Identifier type is required")
  private IdentifierType identifierType;

  @NotNull(message = "Debtor role is required")
  private DebtorRole role;

  /** Type of identifier used. */
  public enum IdentifierType {
    /** Danish personal identification number (personnummer) - 10 digits */
    CPR,
    /** Danish business registration number - 8 digits */
    CVR
  }

  /** The role/capacity in which the debtor incurred the debt. */
  public enum DebtorRole {
    /** Debt incurred as a private individual */
    PERSONAL,
    /** Debt incurred as a business entity (company or sole proprietor) */
    BUSINESS
  }

  /** Creates a personal debtor identifier from CPR. */
  public static DebtorIdentifier personalFromCpr(String cpr) {
    return DebtorIdentifier.builder()
        .identifier(cpr)
        .identifierType(IdentifierType.CPR)
        .role(DebtorRole.PERSONAL)
        .build();
  }

  /** Creates a business debtor identifier from CVR. */
  public static DebtorIdentifier businessFromCvr(String cvr) {
    return DebtorIdentifier.builder()
        .identifier(cvr)
        .identifierType(IdentifierType.CVR)
        .role(DebtorRole.BUSINESS)
        .build();
  }

  /**
   * Creates a sole proprietor debtor identifier from CPR. Used when a natural person has business
   * debt (enkeltmandsvirksomhed).
   */
  public static DebtorIdentifier soleProprietorFromCpr(String cpr) {
    return DebtorIdentifier.builder()
        .identifier(cpr)
        .identifierType(IdentifierType.CPR)
        .role(DebtorRole.BUSINESS)
        .build();
  }

  /**
   * Returns a composite key for uniquely identifying a debtor in a specific role. Format:
   * {identifierType}:{identifier}:{role}
   */
  public String toCompositeKey() {
    return String.format("%s:%s:%s", identifierType, identifier, role);
  }

  /** Validates CPR format (basic check - 10 digits). */
  public boolean isValidCpr() {
    return identifierType == IdentifierType.CPR
        && identifier != null
        && identifier.matches("^[0-9]{10}$");
  }

  /** Validates CVR format (8 digits). */
  public boolean isValidCvr() {
    return identifierType == IdentifierType.CVR
        && identifier != null
        && identifier.matches("^[0-9]{8}$");
  }
}
