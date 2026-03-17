package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.*;

/**
 * Session-backed DTO for the multi-step claim creation wizard. Populated incrementally across
 * wizard steps (debtor identification, claim details, review, submission).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimWizardFormDto implements java.io.Serializable {

  private static final long serialVersionUID = 1L;

  // --- Step 1: Debtor identification ---

  @NotBlank(message = "{wizard.validation.debtorType.required}")
  private String debtorType; // CPR, CVR, SE, AKR

  @NotBlank(message = "{wizard.validation.debtorIdentifier.required}")
  private String debtorIdentifier; // The CPR/CVR/SE/AKR number

  private String debtorFirstName; // For CPR verification

  private String debtorLastName; // For CPR verification

  private boolean debtorVerified;

  private String debtorDisplayName; // Resolved name from registry

  private UUID debtorPersonId; // Resolved person ID from registry

  // --- Step 2: Claim data entry ---

  @NotBlank(message = "{wizard.validation.claimType.required}")
  private String claimType;

  @NotNull(message = "{wizard.validation.amount.required}")
  @Positive(message = "{wizard.validation.amount.positive}")
  private BigDecimal amount; // Residual debt at transfer

  @NotNull(message = "{wizard.validation.principalAmount.required}")
  @Positive(message = "{wizard.validation.principalAmount.positive}")
  private BigDecimal principalAmount; // Original principal

  private String creditorReference; // Unique creditor reference

  @Size(max = 100, message = "{wizard.validation.description.maxlength}")
  private String description; // Free text, max 100 characters

  private LocalDate periodFrom;
  private LocalDate periodTo;
  private LocalDate incorporationDate;
  private LocalDate dueDate;
  private LocalDate lastTimelyPaymentDate;

  @NotNull(message = "{wizard.validation.limitationDate.required}")
  private LocalDate limitationDate; // Required: expiry date

  @NotNull(message = "{wizard.validation.estateProcessing.required}")
  private Boolean estateProcessing; // Required for portal submissions

  private LocalDate courtDate;
  private LocalDate settlementDate;
  private String interestRule;
  private String interestRateCode;
  private BigDecimal interestRate;
  private String claimNote;
  private String debtorNote;
}
