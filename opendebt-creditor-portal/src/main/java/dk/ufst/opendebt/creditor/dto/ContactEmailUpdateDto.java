package dk.ufst.opendebt.creditor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.*;

/** Form DTO for updating the creditor contact email. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactEmailUpdateDto {

  @NotBlank(message = "{settings.email.validation.required}")
  @Email(message = "{settings.email.validation.invalid}")
  private String contactEmail;
}
