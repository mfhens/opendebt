package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.*;

/** Form-backing DTO for manual fordring (debt claim) submission. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FordringFormDto {

  @NotNull(message = "Skyldner-ID skal udfyldes")
  private UUID debtorPersonId;

  @NotNull(message = "Beløb skal udfyldes")
  @Positive(message = "Beløb skal være positivt")
  private BigDecimal principalAmount;

  @NotBlank(message = "Fordringstype skal vælges")
  private String debtTypeCode;

  @NotNull(message = "Forfaldsdato skal udfyldes")
  @FutureOrPresent(message = "Forfaldsdato skal være i dag eller i fremtiden")
  private LocalDate dueDate;

  private String description;
}
