package dk.ufst.opendebt.caseworker.limitation;

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
public class FordringskompleksMemberListData {

  private UUID kompleksId;
  private List<UUID> memberFordringIds;
}
