package dk.ufst.opendebt.debtservice.entity;

/**
 * Interest rule codes mapping to distinct rate-determination strategies.
 *
 * <p>Each code maps to a {@code RATE_*} key in the {@code business_config} table (petition 046).
 */
public enum InterestRuleCode {

  /** Standard inddrivelsesrente: NB udlånsrente + 4%. */
  INDR_STD("RATE_INDR_STD"),

  /** Told (customs) without afdragsordning: NB + 2%. */
  INDR_TOLD("RATE_INDR_TOLD"),

  /** Told with afdragsordning: NB + 1%. */
  INDR_TOLD_AFD("RATE_INDR_TOLD_AFD"),

  /** Interest-exempt (straffebøder): 0%. */
  INDR_EXEMPT(null),

  /**
   * Contractual rate from fordringshaver — uses InterestSelectionEmbeddable.additionalInterestRate.
   */
  INDR_CONTRACT(null),

  /** Opkrævningsrente (pre-collection, informational only). */
  OPK_STD("RATE_OPK_STD");

  private final String configKey;

  InterestRuleCode(String configKey) {
    this.configKey = configKey;
  }

  public String getConfigKey() {
    return configKey;
  }

  public boolean isExempt() {
    return this == INDR_EXEMPT;
  }

  public boolean usesContractualRate() {
    return this == INDR_CONTRACT;
  }
}
