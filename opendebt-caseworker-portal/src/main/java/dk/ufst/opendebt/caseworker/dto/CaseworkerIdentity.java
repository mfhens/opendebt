package dk.ufst.opendebt.caseworker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents a caseworker identity for the demo login flow. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CaseworkerIdentity {

  private String id;
  private String name;
  private String role;
  private String description;
}
