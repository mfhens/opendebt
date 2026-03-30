package dk.ufst.opendebt.rules.model;

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

  public static final int PRIORITY_CHILD_SUPPORT = 1;
  public static final int PRIORITY_TAX = 2;
  public static final int PRIORITY_FINES = 3;
  public static final int PRIORITY_COURT_ORDERED = 4;
  public static final int PRIORITY_OTHER_PUBLIC = 5;
  public static final int PRIORITY_DEFAULT = 10;

  /** ADR-0031: use this constant instead of the raw string literal in DRL rules. */
  public static final String CATEGORY_BOEDER_TVANGSBOEEDER_TILBAGEBETALING =
      "BOEDER_TVANGSBOEEDER_TILBAGEBETALING";
}
