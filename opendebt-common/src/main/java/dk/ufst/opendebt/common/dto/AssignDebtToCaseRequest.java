package dk.ufst.opendebt.common.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignDebtToCaseRequest {

  @NotNull private UUID debtId;

  @NotNull private UUID debtorPersonId;
}
