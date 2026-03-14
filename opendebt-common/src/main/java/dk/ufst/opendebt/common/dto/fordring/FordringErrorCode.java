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
  PERIODE_TIL_EFTER_PERIODE_FRA(569, "PeriodeFra må ikke være efter PeriodeTil");

  // ===== Extension points for petition016 (claimant authorization) =====
  // Error codes: 400, 416, 419, 420, 421, 437, 465, 466, 480, 497, 501, 508, 511, 543

  // ===== Extension points for petition017 (lifecycle/reference) =====
  // Error codes: 418, 428, 429, 434, 469, 470, 471, 473, 474, 477, 488, 493, 494, 496, 498,
  //              502, 503, 504, 506, 526, 527, 530, 538, 539, 540, 541, 542, 544, 546, 547, 570

  // ===== Extension points for petition018 (content validation) =====
  // Error codes: 164, 181, 201, 215, 220, 227, 270, 408, 410, 413, 415, 422, 423, 425, 426,
  //              433, 436, 441, 442, 443, 459, 461, 486, 509, 510, 512, 516, 517, 518, 519,
  //              528, 531, 532, 533, 537, 550, 557, 559, 565, 571, 572, 574, 575, 602, 603

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
