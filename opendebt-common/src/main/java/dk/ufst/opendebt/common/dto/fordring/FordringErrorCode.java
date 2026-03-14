package dk.ufst.opendebt.common.dto.fordring;

import lombok.Getter;

/**
 * Error codes for fordring (claim) validation, mapped to the fordring integration API
 * specification.
 *
 * <p>Error codes are numeric and match the DMI/fordring error code enumeration exactly. Each code
 * has a Danish description for error responses.
 *
 * <p>This enum covers petition015 (core validation) error codes. Extension points are provided for
 * petition016 (claimant authorization), petition017 (lifecycle/reference), and petition018 (content
 * validation) codes.
 */
@Getter
public enum FordringErrorCode {

  // ===== Agreement Validation (petition015) =====
  NO_AGREEMENT_FOUND(2, "Fordringhaveraftale findes ikke"),
  DEBTOR_NOT_FOUND(5, "Skyldner der er angivet findes ikke"),

  // ===== Currency Validation (petition015) =====
  INVALID_CURRENCY(152, "ValutaKode ifølge fordringhaveraftale skal være DKK"),

  // ===== Agreement Type Validation (petition015) =====
  TYPE_AGREEMENT_MISSING(
      151, "Fordringen må ikke indberettes på denne DMIFordringTypeKode ifølge aftalen"),
  NO_SYSTEM_TO_SYSTEM_INTEGRATION(156, "MFAftaleSystemIntegration på fordringhaveraftale er falsk"),

  // ===== Structure Validation (petition015) =====
  GENINDSEND_STRUCTURE_MISSING(403, "MFGenindsendFordringStruktur mangler"),
  OPSKRIV_REGULERING_STRUKTUR(404, "MFOpskrivningReguleringStruktur mangler"),
  OPSKRIV_ANNULLERET_NEDSKRIV_INDBETALING_STRUKTUR(
      406, "MFOpskrivningAnnulleretNedskrivningIndbetalingStruktur mangler"),
  OPSKRIV_OMGJORT_NEDSKRIV_REGULERING_STRUKTUR(
      407, "MFOpskrivningOmgjortNedskrivningReguleringStruktur mangler"),
  VIRKNINGSDATO_MISSING(409, "Virkningsdato skal være udfyldt"),
  FORDRING_TYPE_ERROR(411, "Fordringsart må være inddrivelse (INDR) eller modregning (MODR)"),
  NEDSKRIV_ANNULLERET_OPSKRIV_REGULERING_STRUKTUR(
      412, "MFNedskrivningAnnulleretOpskrivningReguleringStruktur mangler"),
  NEGATIVE_INTEREST_RATE(438, "MerRenteSats kan ikke være negativ"),
  OPRETFORDRING_STRUKTUR(444, "MFOpretFordringStruktur mangler"),
  NEDSKRIV_STRUKTUR_MISSING(447, "MFNedskrivFordringStruktur mangler"),
  TILBAGEKALD_STRUKTUR_MISSING(448, "MFTilbagekaldFordringStruktur mangler"),
  AENDRFORDRING_STRUKTUR_MISSING(458, "MFAendrFordringStruktur mangler"),
  VIRKNINGSDATO_SENERE_END_MODTAGELSE(
      464, "Virkningsdato må ikke være senere end modtagelsestidspunktet"),
  VIRKNINGSDATO_FOER_HOVEDFORDRING_MODTAGELSE(
      467,
      "Virkningsdato for tilbagekald må ikke være tidligere end hovedfordringens"
          + " modtagelsestidspunkt"),
  NEDSKRIV_ANNULLERET_OPSKRIV_INDBETALING_STRUKTUR(
      505, "MFNedskrivningAnnulleretOpskrivningIndbetalingStruktur mangler"),
  NO_FUTURE_VIRKNINGDATO(548, "Fordringens virkningsdato må ikke være fremtidig"),
  TIDLIGST_MULIG_DATO(568, "Der kan ikke registreres datoer der ligger før år 1900"),
  PERIODE_TIL_EFTER_PERIODE_FRA(569, "PeriodeFra må ikke være efter PeriodeTil"),

  // ===== Claimant Authorization Validation (petition016) =====
  SYSTEM_REPORTER_UNAUTHORIZED(
      400, "Systemleverandør der indberetter kan ikke indberette for den angivne fordringshaver"),
  INDR_PERMISSION_MISSING(416, "Fordringshaver må ikke indberette inddrivelsesfordringer"),
  MODR_PERMISSION_MISSING(419, "Fordringshaver må ikke indberette modregningsfordringer"),
  NEDSKRIV_PERMISSION_MISSING(420, "Fordringshaver må ikke indberette nedskrivninger"),
  TILBAGEKALD_PERMISSION_MISSING(421, "Fordringshaver må ikke indberette tilbagekald"),
  PORTAL_AGREEMENT_MISSING(
      437, "Fordringshaver har ikke oprettet en aftale om indberetning via portal"),
  OANI_PERMISSION_MISSING(
      465, "Fordringshaver må ikke indberette OpskrivningAnnulleretNedskrivningIndbetaling"),
  OPSKRIVNING_REGULERING_PERMISSION_MISSING(
      466, "Fordringshaver må ikke indberette Opskrivninger med årsag opskrivningRegulering"),
  SSO_ACCESS_INVALID(480, "Adgang nægtet. Ugyldig sagsbehandler- eller fordringshaver adgang"),
  OONR_PERMISSION_MISSING(
      497, "Fordringshaver må ikke indberette OpskrivningOmgjortNedskrivningRegulering"),
  NAOR_PERMISSION_MISSING(
      501, "Fordringshaver må ikke indberette NedskrivningAnnulleretOpskrivningRegulering"),
  NAOI_PERMISSION_MISSING(
      508, "Fordringshaver må ikke indberette NedskrivningAnnulleretOpskrivningIndbetaling"),
  HOVEDSTOL_PERMISSION_MISSING(511, "Fordringshaver må ikke indberette hovedstolændringer"),
  GENINDSEND_PERMISSION_MISSING(543, "Fordringshaver må ikke indberette genindsend aktioner"),

  // ===== Lifecycle/Reference Validation (petition017) =====

  // Action reference rules
  PENDING_ACTION_EXISTS(418, "Der er allerede indberettet en aktion der ikke er UDFØRT"),
  REFERENCED_FORDRING_REJECTED(428, "Refereret fordringID eller hovedfordringID er afvist"),
  UNKNOWN_AKTION_ID(429, "Aktion refererer til en aktionID som er ukendt for Fordring"),
  WITHDRAWN_DURING_CONVERSION(
      434, "Aktionen vedrører en fordring der er tilbagekaldt i konverteringsprocessen"),

  // Opskrivning/Nedskrivning reference rules
  BELOEB_MISMATCH(469, "FordringOpskrivningBeløb må ikke være forskellig fra nedskrivningsbeløbet"),
  VIRKNINGSDATO_MISMATCH(470, "VirkningFra må ikke være forskellig fra refereret aktion"),
  OANI_WRONG_AARSAGKODE(471, "OANI kræver at FordringNedskrivningÅrsagKode være INDB"),
  NOT_REFERENCING_NEDSKRIV(473, "MFAktionIDRef skal pege på nedskriv aktion"),
  OPSKRIVNING_ON_RENTE(474, "Der kan ikke indberettes opskrivningsfordringer på renter"),
  OONR_WRONG_AARSAGKODE(477, "OONR kræver at FordringNedskrivningÅrsagKode være REGU"),
  DMI_ACTION_NOT_UDFOERT(488, "Annullering af aktion der endnu ikke udført i DMI er ikke tilladt"),
  REFERENCED_ACTION_REJECTED(493, "Den refererede opskrivning/nedskrivning aktion er afvist"),
  FORDRING_ID_MISMATCH(494, "FordringsID på aktionen matcher ikke den refererede aktion"),
  OPSKRIVNING_ON_WITHDRAWN(
      496,
      "Opskrivning på en oprindelig fordring som er tilbagesendt tilbagekaldt eller returneret"),
  NEDSKRIVNING_ON_WITHDRAWN(
      498, "Nedskrivning på en fordringsID som er tilbagesendt tilbagekaldt eller returneret"),
  NAOR_WRONG_REFERENCE_TYPE(
      502, "NAOR aktionens reference skal være OpskrivningRegulering eller OONR"),
  ANNULLERING_ALREADY_EXISTS(503, "Der er allerede modtaget en annullering for denne aktion"),
  NAOR_BELOEB_MISMATCH(504, "FordringNedskrivningBeløb skal være lig med opskrivningsbeløbet"),
  NAOI_WRONG_REFERENCE_TYPE(506, "NAOI aktionens reference skal være af typen OANI"),

  // Action reference rules (continued)
  MF_AKTION_ID_REF_NOT_FOUND(526, "MFAktionIDRef findes ikke"),
  OANI_MISSING_AKTION_ID_REF(527, "MFAktionIDRef er ikke udfyldt for OANI når FordringID er kendt"),
  REFERENCED_CLAIM_WITHDRAWN(530, "Aktionen refererer til en fordring/aktion der er tilbagekaldt"),

  // Tilbagekald rules
  BORT_NOT_ALLOWED_FOR_DMI(538, "BORT tilbagekaldårsag må ikke benyttes til fordringer i DMI"),
  GENINDSEND_INVALID_WITHDRAWAL_REASON(
      539,
      "Genindsend fordring kræver at fordringen er tilbagekaldt med HENS KLAG BORD eller HAFT"),
  GENINDSEND_NOT_WITHDRAWN(540, "Genindsend fordring kræver at fordringen er tilbagekaldt"),
  GENINDSEND_DIFFERENT_FORDRINGSHAVER(
      541, "Genindsend fordring kræver at fordringen genindsendes af samme fordringshaver"),
  GENINDSEND_STAMDATA_MISMATCH(
      542, "Stamdata på aktionen der genindsendes er forskellig fra den oprindelige"),
  GENINDSEND_MODR_NOT_ALLOWED(544, "Der kan ikke genindsendes modregningsfordringer"),
  TILBAGEKALD_FEJL_WITH_VIRKNINGSDATO(
      546, "Virkningsdato må ikke være udfyldt ved tilbagekald med FEJL"),
  EFTERSENDT_REFERENCES_WITHDRAWN(
      547, "Eftersendt fordring har relation til en tilbagekaldt fordring"),
  TILBAGEKALD_HOVEDFORDRING_WITH_UNDIVIDED(
      570,
      "Tilbagekald af hovedfordring kræver at alle opsplittede relaterede fordringer er tilbagekaldt"),

  // ===== Content Validation (petition018) =====

  // Document and Note Validation
  DOCUMENT_SIZE_EXCEEDED(164, "Dokumentets filstørrelse er større end den tilladte grænse"),
  DOCUMENT_COUNT_EXCEEDED(181, "Antal dokumenter indsendt per aktion større end tilladt"),
  EMPTY_NOTE(220, "Sagsbemærkninger på fordringen har ikke noget indhold"),
  NOTE_LENGTH_EXCEEDED(413, "Sagsbemærkninger må max være på det tilladte antal tegn"),
  DISALLOWED_DOCUMENT_TYPE(415, "Dokumentets filtype er ikke tilladt"),
  DOCUMENTS_ON_UNDERLYING_FORDRING(
      516, "Aktionen refererer til en underliggende fordring der indeholder dokumenter"),

  // Claim Amount Validation
  HOVEDFORDRING_KATEGORI_MISSING(201, "En hovedfordring skal have fordringtypekategori HF"),
  CLAIM_AMOUNT_BELOW_LIMIT(215, "Fordringsbeløb ikke større end nedre grænse"),
  ZERO_CORRECTION_AMOUNT(227, "Korrektion på kr. 0 ikke muligt"),
  NEDSKRIV_BELOEB_NOT_POSITIVE(408, "Nedskrivningsbeløb skal være større end 0"),
  MULTIPLE_HOVEDFORDRINGER(425, "Der kan kun oprettes en hovedfordring per aktion"),

  // Sub-Claim Validation
  SUBCLAIM_ART_TYPE_MISMATCH(270, "Underfordringen matcher ikke art med hovedfordring"),
  SUBCLAIM_TYPE_NOT_ALLOWED(459, "Underfordringstypen er ikke tilladt for fordringshaver"),
  RELATED_CLAIM_MISSING_HOVEDFORDRING_ID(
      461, "HovedfordringsID ikke angivet eller eksisterende for relateret fordring"),
  HOVEDFORDRING_ID_FILLED_BY_FORDRINGSHAVER(
      423, "HovedfordringID må ikke udfyldes af fordringshaver for relateret fordring"),

  // Interest Validation
  MER_RENTESATS_INVALID_FOR_KODE(
      436, "MerRenteSats må kun benyttes med RenteSatsKode 03, 04 eller 07"),
  RENTE_REGEL_002_INVALID_COMBINATION(
      441, "Rente regel 002 kan kun bruges med RenteSatsKode 99 og MerRenteSats 00"),
  RENTE_SATS_KODE_INVALID_FOR_PSRM(442, "RenteSatsKode kan ikke benyttes i PSRM"),
  INTEREST_ON_NON_BEARING_TYPE(443, "Fordringstypen er ikke rentebærende"),

  // Nedskriv Reason Validation
  REGU_AT_HAEFTELSE_LEVEL(410, "Årsagskoden REGU kan kun benyttes på fordringsniveau"),
  NEDSKRIV_REQUIRES_DEBTOR(433, "Nedskrivningstype kræver at skyldners identitet er påkrævet"),
  REGU_WITH_VIRKNINGSDATO(519, "REGU nedskrivning må ikke medsendes en virkningsdato"),
  FAST_FOR_PSRM_CLAIM(571, "ÅrsagKode FAST kan ikke benyttes for PSRM fordring"),

  // Hovedstol Validation
  NEW_HOVEDSTOL_NOT_HIGHER(510, "Ny hovedstol er lavere eller ens med eksisterende"),
  HOVEDSTOL_CHANGE_FOR_DMI(512, "DMI understøtter ikke ændring af hovedstol"),
  FHI_STRUKTUR_MISSING(517, "FejlagtigHovedstolIndberetningStruktur mangler for hovedstolændring"),
  HOVEDSTOL_CHANGE_ON_WITHDRAWN(518, "Hovedstolændring på tilbagekaldt fordring ikke tilladt"),

  // Hæftelse Validation
  DUPLICATE_HAEFTERE(528, "Fordringen har hæfter hvor hæfteren er den samme"),
  HAEFTELSE_DOM_ID_WITHOUT_DATE(531, "Fordringen har et HaeftelseDomId men ingen HaeftelseDomDato"),
  HAEFTELSE_DOM_DATE_WITHOUT_ID(
      532, "Fordringen har ikke et HaeftelseDomId men har alligevel en HaeftelseDomDato"),
  FUTURE_DOM_DATE(533, "Domsdato eller Forligsdato kan ikke være i fremtiden"),
  DMI_MISMATCHED_SRB_FORFALD(
      557, "SRB og Forfald er forskellig på to hæftere for DMI-routed fordring"),
  DMI_HAEFTELSE_DOCUMENTS(559, "DMI understøtter ikke dokumenter på hæftelsesniveau"),

  // Routing Validation
  SYNC_PORTAL_DMI(422, "Synkrone aktioner via portalen på fordringer i DMI er ikke understøttet"),
  NAOR_TO_DMI(426, "NedskrivningAnnulleretOpskrivningRegulering understøttes ikke af DMI"),
  DMI_INVALID_AKR_LENGTH(
      565, "Fordringen har en skyldner med et PSRM AKR-nummer for langt til DMI"),
  FORELOEBIG_FASTSAT_TO_PSRM(572, "Fordring med forløbig fastsat kan ikke sendes til PSRM"),

  // Claim Type Validation
  UNKNOWN_FORDRING_ID(509, "FordringID er ikke kendt af Fordring"),
  AENDRFORDRING_ON_INDR(537, "FordringAendr aktioner på inddrivelsesfordringer er ikke tilladt"),
  MISSING_REQUIRED_STAMDATA(550, "Stamdata felter på fordringsaktionen mangler"),
  INACTIVE_CLAIM_TYPE_PSRM(574, "Fordringstypen er inaktiv i PSRM"),
  MISSING_REQUIRED_BFE(575, "Feltet BFE skal være udfyldt for denne fordringstype"),

  // Identifier Validation
  NON_UNIQUE_FORDRINGSHAVER_REF_ID(486, "FordringshaverRefID er ikke entydig"),
  MODIFICATION_OF_ERROR_WITHDRAWN(
      602, "Fordringen er allerede tilbagekaldt/tilbagesend med årsagskode FEJL"),
  NON_FEJL_WITHDRAWAL_AFTER_HENS_KLAG_BORD(
      603, "Fordringen kan kun returneres eller tilbagesendes med FEJL efter HENS/KLAG/BORD");

  private final int code;
  private final String danishDescription;

  FordringErrorCode(int code, String danishDescription) {
    this.code = code;
    this.danishDescription = danishDescription;
  }

  /**
   * Look up an error code by its numeric value.
   *
   * @param code the numeric error code
   * @return the corresponding FordringErrorCode
   * @throws IllegalArgumentException if no matching code is found
   */
  public static FordringErrorCode fromCode(int code) {
    for (FordringErrorCode errorCode : values()) {
      if (errorCode.code == code) {
        return errorCode;
      }
    }
    throw new IllegalArgumentException("Ukendt fordring fejlkode: " + code);
  }
}
