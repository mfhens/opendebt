package dk.ufst.opendebt.debtservice.limitation.dto;

import java.util.UUID;

import dk.ufst.opendebt.debtservice.limitation.entity.ForaeldelseStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectionRegistrationResult {

  private UUID indsigelsesId;
  private ForaeldelseStatus status;
}
