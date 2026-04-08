package dk.ufst.opendebt.debtservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response item for {@code GET /internal/debtors/{debtorId}/fordringer/active}.
 *
 * <p>Carries all financial components required by payment-service to execute dækningsrækkefølge per
 * P057 and GIL § 4. Fields are named consistently with the {@code DaekningFordringEntity}
 * vocabulary used in payment-service so that the interim cache can be replaced directly.
 *
 * <p>GDPR: contains no CPR, name, or address — only technical identifiers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveFordringResponseDto {

  /** Unique identifier of the fordring (UUID, = {@code debts.id}). */
  private UUID fordringId;

  /**
   * Debt-type code, e.g. {@code "600"} (SKAT A-skat), {@code "700"} (MOMS). Matches the {@code
   * debt_type_code} column; interpreted by payment-service for priority category mapping.
   */
  private String fordringType;

  /** Remaining balance (udestående saldo) after write-downs and payments. */
  private BigDecimal beloebResterende;

  /** Opkrævningsrenter component (STK2 rate), stored at claim receipt. */
  private BigDecimal opkraevningsrenter;

  /**
   * Inddrivelsesrenter – fordringshaver (previously labelled _STK3, renamed per GIL terminology).
   */
  private BigDecimal inddrivelsesrenterFordringshaver;

  /** Inddrivelsesrenter accrued before any reversal (tilbageføring). */
  private BigDecimal inddrivelsesrenterFoerTilbagefoersel;

  /** Inddrivelsesrenter – stk. 1 component. */
  private BigDecimal inddrivelsesrenterStk1;

  /** Øvrige renter from PSRM legacy system. */
  private BigDecimal oevrigeRenterPsrm;

  /**
   * Inddrivelsesomkostninger (fees). Mapped from {@code debts.fees_amount} — covers rykkergebyr,
   * udlægsomkostninger, lønindeholdelsesgebyr.
   */
  private BigDecimal inddrivelsesomkostninger;

  /**
   * Sequence number determining application order within the debtor's portfolio. Lower numbers are
   * applied first. Null if not yet assigned (fallback: FIFO by {@link #applicationTimestamp}).
   */
  private Integer sekvensNummer;

  /**
   * {@code true} when this fordring is currently under an active lønindeholdelse (wage-garnishment)
   * measure. Derived from {@code collection_measures} — not stored as a column to avoid
   * denormalisation drift.
   */
  private Boolean inLoenindeholdelsesIndsats;

  /**
   * If this fordring is an opskrivning (upward adjustment) of a previous fordring, this field
   * contains that fordring's id. Mapped from {@code debts.parent_claim_id}.
   */
  private UUID opskrivningAfFordringId;

  /** Creditor organisation ID ({@code debts.creditor_org_id}). Never CVR directly. */
  private UUID fordringshaverId;

  /** GIL legal-paragraph reference, e.g. {@code "GIL § 4, stk. 1"}. */
  private String gilParagraf;

  /**
   * Timestamp when the fordring was received for collection (FIFO tie-breaker). Derived from {@code
   * debts.received_at} projected to UTC offset.
   */
  private OffsetDateTime applicationTimestamp;
}
