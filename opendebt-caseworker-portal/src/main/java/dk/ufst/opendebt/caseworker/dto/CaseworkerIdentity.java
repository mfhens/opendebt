package dk.ufst.opendebt.caseworker.dto;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents a caseworker identity for the demo login flow. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CaseworkerIdentity implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private String id;
  private String name;
  private String role;
  private String description;
}
