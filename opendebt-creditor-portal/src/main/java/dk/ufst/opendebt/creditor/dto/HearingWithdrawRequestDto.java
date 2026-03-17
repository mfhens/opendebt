package dk.ufst.opendebt.creditor.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.*;

/** DTO for withdrawing a hearing claim. Requires a reason. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HearingWithdrawRequestDto {

  @NotBlank private String reason;
}
