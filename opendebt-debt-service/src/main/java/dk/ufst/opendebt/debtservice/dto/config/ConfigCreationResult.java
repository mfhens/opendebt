package dk.ufst.opendebt.debtservice.dto.config;

import java.util.List;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigCreationResult {
  private ConfigEntryDto created;
  private List<ConfigEntryDto> derivedEntries; // non-null only when RATE_NB_UDLAAN was created
}
