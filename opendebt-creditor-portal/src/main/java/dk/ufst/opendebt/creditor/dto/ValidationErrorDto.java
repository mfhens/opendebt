package dk.ufst.opendebt.creditor.dto;

import java.io.Serial;
import java.io.Serializable;

import lombok.*;

/** DTO representing a single validation error with numeric code and description. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorDto implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private int errorCode;
  private String description;
}
