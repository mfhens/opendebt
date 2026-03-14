package dk.ufst.opendebt.common.dto.fordring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object for fordring (claim action) validation.
 *
 * <p>Carries the pre-parsed claim action data needed for Drools rule evaluation. Structure presence
 * flags indicate which MF*Struktur elements were present in the original submission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FordringValidationRequest {

  // ===== Action identification =====

  /** The action type code (e.g., OPRETFORDRING, GENINDSENDFORDRING, NEDSKRIV). */
  private String aktionKode;

  // ===== Structure presence flags =====

  private boolean mfOpretFordringStrukturPresent;
  private boolean mfGenindsendFordringStrukturPresent;
  private boolean mfNedskrivFordringStrukturPresent;
  private boolean mfTilbagekaldFordringStrukturPresent;
  private boolean mfAendrFordringStrukturPresent;
  private boolean mfOpskrivningReguleringStrukturPresent;
  private boolean mfOpskrivningAnnulleretNedskrivningIndbetalingStrukturPresent;
  private boolean mfOpskrivningOmgjortNedskrivningReguleringStrukturPresent;
  private boolean mfNedskrivningAnnulleretOpskrivningReguleringStrukturPresent;
  private boolean mfNedskrivningAnnulleretOpskrivningIndbetalingStrukturPresent;

  // ===== Currency and type =====

  /** Currency code, must be DKK. */
  private String valutaKode;

  /** Art type: INDR (inddrivelse) or MODR (modregning). */
  private String artType;

  // ===== Interest =====

  /** Additional interest rate (MerRenteSats). Must be non-negative. */
  private BigDecimal merRenteSats;

  // ===== Dates =====

  /** Effective date (VirkningsDato). */
  private LocalDate virkningsDato;

  /** Receipt timestamp (ModtagelsesTidspunkt). */
  private LocalDateTime modtagelsesTidspunkt;

  /** Period start date. */
  private LocalDate periodeFra;

  /** Period end date. */
  private LocalDate periodeTil;

  /** Whether VirkningsDato is required for this action type. */
  private boolean virkningsDatoRequired;

  // ===== Agreement =====

  /** Creditor agreement ID. */
  private String fordringhaveraftaleId;

  /** Whether the agreement was found in the system. */
  private boolean agreementFound;

  /** Claim type code from DMI. */
  private String dmiFordringTypeKode;

  /** Whether the claim type is allowed by the agreement. */
  private boolean claimTypeAllowedByAgreement;

  /** Whether MFAftaleSystemIntegration is set on the agreement. */
  private boolean mfAftaleSystemIntegration;

  // ===== Submission context =====

  /** Whether this is a system-to-system (M2M) submission. */
  private boolean systemToSystem;

  // ===== Debtor =====

  /** Debtor identifier string. */
  private String debtorId;

  // ===== Withdrawal context (for rule 467) =====

  /** Receipt timestamp of the main claim being withdrawn, if applicable. */
  private LocalDateTime hovedfordringModtagelsesTidspunkt;

  // ===== Authorization context (petition016) =====

  /** The system reporter ID for M2M submissions. */
  private String systemReporterId;

  /** Whether the system reporter is authorized for this fordringshaver. */
  private boolean systemReporterAuthorized;

  /** The fordringshaver ID submitting the action. */
  private String fordringshaverKode;

  /** Whether the fordringshaver has permission to submit INDR claims. */
  private boolean hasIndrPermission;

  /** Whether the fordringshaver has permission to submit MODR claims. */
  private boolean hasModrPermission;

  /** Whether the fordringshaver has permission to perform nedskriv actions. */
  private boolean hasNedskrivPermission;

  /** Whether the fordringshaver has permission to perform tilbagekald actions. */
  private boolean hasTilbagekaldPermission;

  /** Whether the fordringshaver has a portal submission agreement. */
  private boolean hasPortalAgreement;

  /** Whether the submission is via the fordringshaverportal. */
  private boolean portalSubmission;

  /** Whether the fordringshaver has OANI permission. */
  private boolean hasOaniPermission;

  /** Whether the fordringshaver has opskrivning regulering permission. */
  private boolean hasOpskrivningReguleringPermission;

  /** Whether the fordringshaver has OONR permission. */
  private boolean hasOonrPermission;

  /** Whether the fordringshaver has NAOR permission. */
  private boolean hasNaorPermission;

  /** Whether the fordringshaver has NAOI permission. */
  private boolean hasNaoiPermission;

  /** Whether the fordringshaver has permission to modify hovedstol. */
  private boolean hasHovedstolPermission;

  /** Whether the request modifies hovedstol (for AENDRFORDRING). */
  private boolean modifyingHovedstol;

  /** Whether the fordringshaver has permission to resubmit claims. */
  private boolean hasGenindsendPermission;

  /** Portal user ID for SSO validation. */
  private String portalUserId;

  /** Whether the portal user has valid SSO access. */
  private boolean validSsoAccess;

  // ===== Lifecycle context (petition017) =====

  // Genindsend (resubmit) context
  /** The withdrawal reason code of the claim being resubmitted. */
  private String originalWithdrawalReason;

  /** Whether the claim being resubmitted is actually withdrawn. */
  private boolean originalClaimWithdrawn;

  /** The fordringshaver ID of the original claim being resubmitted. */
  private String originalFordringshaverKode;

  /** Whether stamdata matches the original claim for resubmission. */
  private boolean stamdataMatchesOriginal;

  /** Whether the original claim is a MODR (modregning) claim. */
  private boolean originalClaimIsModr;

  // Tilbagekald (withdrawal) context
  /** Whether the claim was withdrawn during the conversion process. */
  private boolean withdrawnDuringConversion;

  /** Whether the claim is routed to DMI. */
  private boolean routedToDmi;

  /** The tilbagekald (withdrawal) reason code. */
  private String tilbagekaldAarsagKode;

  /** Whether the referenced hovedfordring (main claim) is withdrawn. */
  private boolean hovedfordringWithdrawn;

  /** Whether there are un-withdrawn divided related claims. */
  private boolean hasUnwithdrawDividedClaims;

  // Action reference context
  /** Whether there is a pending action that is not yet UDFØRT. */
  private boolean hasPendingAction;

  /** Whether the referenced AktionID exists in the system. */
  private boolean referencedAktionExists;

  /** The ID of the referenced action (MFAktionIDRef). */
  private String mfAktionIdRef;

  /** Whether the MFAktionIDRef exists in the system. */
  private boolean mfAktionIdRefExists;

  /** Whether MFAktionIDRef is required but missing. */
  private boolean mfAktionIdRefRequired;

  /** Whether the referenced claim/action is withdrawn. */
  private boolean referencedClaimWithdrawn;

  // Opskrivning/Nedskrivning reference context
  /** The amount on the referenced nedskrivning action. */
  private BigDecimal referencedNedskrivningBeloeb;

  /** The opskrivning amount on the current action. */
  private BigDecimal opskrivningBeloeb;

  /** The nedskrivning amount on the current action. */
  private BigDecimal nedskrivningBeloeb;

  /** VirkningFra date on the referenced action. */
  private LocalDate referencedVirkningFra;

  /** The årsag kode on the referenced nedskrivning. */
  private String referencedNedskrivningAarsagKode;

  /** The type of the referenced action. */
  private String referencedActionType;

  /** Whether the claim is an interest claim (rente). */
  private boolean claimIsRente;

  /** Whether the referenced opskrivning/nedskrivning action is rejected. */
  private boolean referencedActionRejected;

  /** The FordringID on the referenced action. */
  private String referencedFordringId;

  /** Whether there is already an existing annullering for the target action. */
  private boolean existingAnnulleringForAction;

  /** The opskrivning beløb on the referenced action (for NAOR). */
  private BigDecimal referencedOpskrivningBeloeb;

  // State validation context
  /** Whether the referenced FordringID is rejected. */
  private boolean referencedFordringRejected;

  /** Whether the referenced action is in DMI and not yet UDFØRT. */
  private boolean referencedActionInDmiNotUdfoert;

  /** Whether the original claim for opskrivning is withdrawn. */
  private boolean originalClaimForOpskrivningWithdrawn;

  /** Whether the FordringID for nedskrivning is withdrawn. */
  private boolean fordringIdForNedskrivningWithdrawn;

  // ===== Content validation context (petition018) =====

  // Document and Note validation
  /** Size of the largest document in bytes. */
  private Long maxDocumentSizeBytes;

  /** Maximum allowed document size in bytes. */
  private Long maxAllowedDocumentSizeBytes;

  /** Number of documents in the action. */
  private Integer documentCount;

  /** Maximum allowed documents per action. */
  private Integer maxAllowedDocuments;

  /** Whether a note/sagsbemærkning is empty. */
  private boolean hasEmptyNote;

  /** Length of the longest note. */
  private Integer noteLength;

  /** Maximum allowed note length. */
  private Integer maxAllowedNoteLength;

  /** Whether a document has a disallowed type. */
  private boolean hasDisallowedDocumentType;

  /** Whether OANI/OONR/OpRegu references fordring with docs/notes. */
  private boolean underlyingFordringHasDocuments;

  // Claim Amount validation
  /** Whether hovedfordring has category HF. */
  private boolean hovedfordringHasKategoriHf;

  /** The claim amount. */
  private BigDecimal claimAmount;

  /** Lower limit for claim amount. */
  private BigDecimal claimAmountLowerLimit;

  /** Whether this is an opskrivning/nedskrivning with zero correction. */
  private boolean hasZeroCorrectionAmount;

  /** The nedskrivning beløb for NEDSKRIV actions. */
  private BigDecimal nedskrivningBeloebValue;

  /** Number of hovedfordringer in the action. */
  private Integer hovedfordringCount;

  // Sub-Claim validation
  /** Whether sub-claim art type matches hovedfordring. */
  private boolean subclaimArtTypeMatches;

  /** Whether there is a sub-claim with mismatched art type. */
  private boolean hasSubclaimArtTypeMismatch;

  /** Whether sub-claim type is allowed for fordringshaver. */
  private boolean subclaimTypeAllowed;

  /** Whether related claim is missing HovedfordringID. */
  private boolean relatedClaimMissingHovedfordringId;

  /** Whether HovedfordringID was filled by fordringshaver (not allowed). */
  private boolean hovedfordringIdFilledByFordringshaver;

  // Interest validation
  /** The RenteSatsKode. */
  private String renteSatsKode;

  /** Whether MerRenteSats is specified with invalid RenteSatsKode. */
  private boolean merRenteSatsWithInvalidKode;

  /** The RenteRegel code. */
  private String renteRegel;

  /** Whether RenteRegel 002 has invalid combination. */
  private boolean renteRegel002InvalidCombination;

  /** Whether RenteSatsKode is invalid for PSRM. */
  private boolean renteSatsKodeInvalidForPsrm;

  /** Whether interest is specified on non-interest-bearing claim type. */
  private boolean interestOnNonBearingType;

  /** Whether the claim targets PSRM. */
  private boolean targetsPsrm;

  // Nedskriv Reason validation
  /** Whether ÅrsagKode is REGU at hæftelse level. */
  private boolean reguAtHaeftelseLevel;

  /** The nedskrivning årsag kode. */
  private String nedskrivAarsagKode;

  /** Whether nedskrivning requires debtor identity. */
  private boolean nedskrivRequiresDebtor;

  /** Whether debtor identity is provided for nedskrivning. */
  private boolean nedskrivDebtorProvided;

  /** Whether REGU nedskrivning has VirkningsDato. */
  private boolean reguWithVirkningsDato;

  /** Whether ÅrsagKode is FAST for PSRM claim. */
  private boolean fastAarsagForPsrmClaim;

  // Hovedstol validation
  /** Current hovedstol amount. */
  private BigDecimal currentHovedstol;

  /** New hovedstol amount being set. */
  private BigDecimal newHovedstol;

  /** Whether FejlagtigHovedstolIndberetningStruktur is present. */
  private boolean fhiStrukturPresent;

  /** Whether the claim is withdrawn (for hovedstol change). */
  private boolean claimIsWithdrawn;

  // Hæftelse validation
  /** Whether there are duplicate hæftere. */
  private boolean hasDuplicateHaeftere;

  /** Whether HaeftelseDomId is present without date. */
  private boolean haeftelseDomIdWithoutDate;

  /** Whether HaeftelseDomDato is present without id. */
  private boolean haeftelseDomDateWithoutId;

  /** Whether DomsDato or ForligsDato is in the future. */
  private boolean hasFutureDomDate;

  /** Whether DMI claim has mismatched SRB/Forfald across hæftere. */
  private boolean dmiMismatchedSrbForfald;

  /** Whether DMI claim has hæftelse-level documents. */
  private boolean dmiHaeftelseDocuments;

  // Routing validation
  /** Whether this is a synchronous portal action for DMI claim. */
  private boolean syncPortalActionForDmi;

  /** Whether AKR number exceeds allowed length for DMI. */
  private boolean akrLengthExceedsForDmi;

  /** Whether claim is ForeløbigFastsat and targets PSRM. */
  private boolean foreloebigFastsatToPsrm;

  // Claim Type validation
  /** Whether FordringID is known by the system. */
  private boolean fordringIdKnown;

  /** Whether this is AENDRFORDRING on INDR claim. */
  private boolean aendrfordringOnIndr;

  /** Whether required stamdata is missing. */
  private boolean missingRequiredStamdata;

  /** Whether claim type is inactive in PSRM. */
  private boolean claimTypeInactiveInPsrm;

  /** Whether required BFE field is missing. */
  private boolean missingRequiredBfe;

  // Identifier validation
  /** Whether FordringshaverRefID is non-unique. */
  private boolean nonUniqueFordringshaverRefId;

  /** Whether claim was already withdrawn with FEJL reason. */
  private boolean alreadyWithdrawnWithFejl;

  /** Whether previous withdrawal was HENS/KLAG/BORD and current is non-FEJL. */
  private boolean nonFejlWithdrawalAfterHensKlagBord;
}
