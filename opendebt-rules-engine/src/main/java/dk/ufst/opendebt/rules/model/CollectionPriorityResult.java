package dk.ufst.opendebt.rules.model;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/** Result of collection priority evaluation. */
@Data
@Builder
public class CollectionPriorityResult {

  private UUID debtId;
  private int priorityRank;
  private String priorityCategory;
  private String legalBasis;

  /**
   * Within underholdsbidrag (category 3): 0 = not underholdsbidrag; 1 = privatretlig (covered
   * first, FR-1.3c); 2 = offentlig (FR-1.3d).
   *
   * <p>Catala: {@code underholdsbidragOrdning} from PrioritetKategoriRang scope.
   */
  private int underholdsbidragOrdning;

  /**
   * FIFO sort key for ordering within the same priority category (GIL § 4, stk. 2).
   *
   * <p>Catala: {@code fifoSortKey} from FifoSortNøgle scope. Null when no {@code oprettetDato} was
   * provided on the request.
   */
  private LocalDate fifoSortKey;

  // -----------------------------------------------------------------------
  // GIL § 4 priority rank constants — corrected by TB-034
  // Catala source: ga_2_3_2_1_daekningsraekkefoeigen.catala_da (G.A. v3.16)
  // -----------------------------------------------------------------------

  /** GIL § 6a, stk. 1 — Rimelige omkostninger (category 1, highest). */
  public static final int GIL4_RIMELIGE_OMKOSTNINGER = 1;

  /**
   * GIL § 10b; lov nr. 288/2022 — Bøder, tvangsbøder og tilbagebetalingskrav (category 2).
   *
   * <p>Catala: BOEDER_TVANGSBOEEDER_TILBAGEBETALING → prioritetRang = 2 (FR-1.2). Previously
   * (incorrectly) assigned rank 3 in collection-priority.drl — fixed by TB-034.
   */
  public static final int GIL4_BOEDER_TVANGSBOEEDER_TILBAGEBETALING = 2;

  /**
   * GIL § 4, stk. 1, nr. 2 — Underholdsbidrag (category 3). Privatretlig covered before offentlig
   * via {@code underholdsbidragOrdning}.
   *
   * <p>Catala: UNDERHOLDSBIDRAG_PRIVATRETLIG / _OFFENTLIG → prioritetRang = 3 (FR-1.3). Previously
   * (incorrectly) assigned rank 1 — fixed by TB-034.
   */
  public static final int GIL4_UNDERHOLDSBIDRAG = 3;

  /**
   * GIL § 4, stk. 1, nr. 3 — Andre fordringer (category 4, lowest, includes tax debts).
   *
   * <p>Catala: ANDRE_FORDRINGER → prioritetRang = 4 (FR-1.4). Tax debt previously (incorrectly)
   * assigned rank 2 — fixed by TB-034.
   */
  public static final int GIL4_ANDRE_FORDRINGER = 4;

  /** Default rank used before rule evaluation; not a GIL § 4 category. */
  public static final int PRIORITY_DEFAULT = 10;

  /** ADR-0031: use this constant instead of the raw string literal in DRL rules. */
  public static final String CATEGORY_BOEDER_TVANGSBOEEDER_TILBAGEBETALING =
      "BOEDER_TVANGSBOEEDER_TILBAGEBETALING";
}
