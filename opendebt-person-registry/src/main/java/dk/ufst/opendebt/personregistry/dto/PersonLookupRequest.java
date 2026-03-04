package dk.ufst.opendebt.personregistry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import lombok.Builder;
import lombok.Data;

/** Request to lookup or create a person by their identifier. */
@Data
@Builder
public class PersonLookupRequest {

  @NotBlank(message = "Identifier is required")
  @Pattern(regexp = "^[0-9]{8,10}$", message = "Must be valid CPR (10 digits) or CVR (8 digits)")
  private String identifier;

  @NotNull(message = "Identifier type is required")
  private IdentifierType identifierType;

  @NotNull(message = "Role is required")
  private PersonRole role;

  // Optional: provide name/address if creating new person
  private String name;
  private String addressStreet;
  private String addressCity;
  private String addressPostalCode;
  private String addressCountry;
  private String email;
  private String phone;

  public enum IdentifierType {
    CPR,
    CVR
  }

  public enum PersonRole {
    PERSONAL,
    BUSINESS
  }
}
