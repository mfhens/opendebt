package dk.ufst.opendebt.caseworker.limitation;

import java.time.LocalDate;
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
public class LimitationPanelData {

  private UUID fordringId;
  private LocalDate currentFristExpires;
  private LocalDate udskydelseDato;
  private Boolean isInUdskydelse;
  private String status;
  private UUID kompleksId;
  private List<UUID> memberFordringIds;
  private List<AfbrydelsePanelRow> afbrydelseHistory;
  private List<TillaegsfristPanelRow> tillaegsfristHistory;
  private String objectionRationale;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AfbrydelsePanelRow {
    private String type;
    private LocalDate eventDate;
    private String legalReference;
    private LocalDate newFristExpires;
    private UUID sourceFordringId;
    private UUID targetFordringId;
    private String propagationReason;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TillaegsfristPanelRow {
    private String type;
    private LocalDate appliedDate;
    private Integer extensionYears;
    private LocalDate newFristExpires;
  }
}
