package dk.ufst.opendebt.creditor.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.*;

/** DTO for approving a hearing claim. Requires a written justification (aarsag). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HearingApproveRequestDto {

  @NotBlank private String justification;
}
