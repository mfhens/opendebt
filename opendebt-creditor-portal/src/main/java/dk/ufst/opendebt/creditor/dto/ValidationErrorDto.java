package dk.ufst.opendebt.creditor.dto;

import lombok.*;

/** DTO representing a single validation error with numeric code and description. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorDto {

  private int errorCode;
  private String description;
}
