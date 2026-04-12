package dk.ufst.opendebt.debtservice.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.debtservice.service.ClaimValidationContext;

/** Maps the debt-service submission contract onto the shared fordring Drools request model. */
@Component
public class ClaimValidationRequestMapper {

  private static final String DEFAULT_ACTION = "OPRETFORDRING";
  private static final String DEFAULT_CURRENCY = "DKK";
  private static final String DEFAULT_ART_TYPE = "INDR";
  private static final int MAX_ALLOWED_NOTE_LENGTH = 4000;

  public FordringValidationRequest toRequest(DebtDto claim, ClaimValidationContext context) {
    LocalDateTime receivedAt = LocalDateTime.now();
    String artType = normalizeArtType(claim.getClaimArt());
    String longestNote = longestNote(claim);

    return FordringValidationRequest.builder()
        .aktionKode(DEFAULT_ACTION)
        .mfOpretFordringStrukturPresent(true)
        .valutaKode(DEFAULT_CURRENCY)
        .artType(artType)
        .merRenteSats(defaultAmount(claim.getAdditionalInterestRate()))
        .virkningsDato(resolveEffectiveDate(claim, receivedAt.toLocalDate()))
        .modtagelsesTidspunkt(receivedAt)
        .virkningsDatoRequired(true)
        .periodeFra(claim.getPeriodFrom())
        .periodeTil(claim.getPeriodTo())
        .fordringhaveraftaleId(claim.getCreditorId())
        .agreementFound(hasText(claim.getCreditorId()))
        .dmiFordringTypeKode(claim.getDebtTypeCode())
        .claimTypeAllowedByAgreement(hasText(claim.getDebtTypeCode()))
        .mfAftaleSystemIntegration(
            !context.isSystemToSystemSubmission() || context.systemReporterAuthorized())
        .systemToSystem(context.isSystemToSystemSubmission())
        .debtorId(claim.getDebtorId())
        .systemReporterId(context.isSystemToSystemSubmission() ? claim.getCreditorId() : null)
        .systemReporterAuthorized(context.systemReporterAuthorized())
        .fordringshaverKode(claim.getCreditorId())
        .hasIndrPermission(!"INDR".equals(artType) || context.claimCreationAuthorized())
        .hasModrPermission(!"MODR".equals(artType) || context.claimCreationAuthorized())
        .hasPortalAgreement(!context.isPortalSubmission() || context.portalAgreement())
        .portalSubmission(context.isPortalSubmission())
        .portalUserId(context.isPortalSubmission() ? claim.getCreditorId() : null)
        .validSsoAccess(!context.isPortalSubmission() || context.validSsoAccess())
        .hovedfordringHasKategoriHf(true)
        .claimAmount(claim.getPrincipalAmount())
        .claimAmountLowerLimit(BigDecimal.ZERO)
        .hovedfordringCount(1)
        .subclaimTypeAllowed(true)
        .fordringIdKnown(true)
        .missingRequiredStamdata(hasMissingRequiredStamdata(claim))
        .renteSatsKode(claim.getInterestRateCode())
        .renteRegel(claim.getInterestRule())
        .hasEmptyNote(hasEmptyNote(claim))
        .noteLength(longestNote != null ? longestNote.length() : null)
        .maxAllowedNoteLength(MAX_ALLOWED_NOTE_LENGTH)
        .build();
  }

  private LocalDate resolveEffectiveDate(DebtDto claim, LocalDate today) {
    if (claim.getInceptionDate() != null) {
      return claim.getInceptionDate();
    }
    if (claim.getPeriodFrom() != null) {
      return claim.getPeriodFrom();
    }
    if (claim.getDueDate() != null && !claim.getDueDate().isAfter(today)) {
      return claim.getDueDate();
    }
    return today.minusDays(1);
  }

  private String normalizeArtType(String claimArt) {
    if (!hasText(claimArt)) {
      return DEFAULT_ART_TYPE;
    }
    return claimArt.trim().toUpperCase(Locale.ROOT);
  }

  private BigDecimal defaultAmount(BigDecimal amount) {
    return amount != null ? amount : BigDecimal.ZERO;
  }

  private boolean hasMissingRequiredStamdata(DebtDto claim) {
    return claim.getPrincipalAmount() == null
        || !hasText(claim.getDebtorId())
        || !hasText(claim.getDebtTypeCode())
        || (claim.getDueDate() == null && claim.getPaymentDeadline() == null);
  }

  private boolean hasEmptyNote(DebtDto claim) {
    return Stream.of(claim.getClaimNote(), claim.getCustomerNote())
        .anyMatch(note -> note != null && note.isBlank());
  }

  private String longestNote(DebtDto claim) {
    return Stream.of(claim.getClaimNote(), claim.getCustomerNote())
        .filter(this::hasText)
        .max(java.util.Comparator.comparingInt(String::length))
        .orElse(null);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
