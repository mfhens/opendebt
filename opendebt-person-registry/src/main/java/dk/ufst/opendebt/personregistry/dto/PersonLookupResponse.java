package dk.ufst.opendebt.personregistry.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * Response from person lookup containing only the technical ID. This is what other services store -
 * NO PII.
 */
@Data
@Builder
public class PersonLookupResponse {

  /**
   * The technical UUID to use for referencing this person. This is the ONLY identifier other
   * services should store.
   */
  private UUID personId;

  /** Whether this was an existing person or newly created. */
  private boolean created;

  /** The role context for this person reference. */
  private String role;
}
