package dk.ufst.opendebt.debtservice.limitation.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFordringskompleksRequest {

  private List<UUID> memberFordringIds;
}
