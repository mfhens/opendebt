package dk.ufst.opendebt.debtservice.limitation.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import dk.ufst.opendebt.debtservice.limitation.entity.ForaeldelseStatus;
import dk.ufst.opendebt.debtservice.limitation.entity.Retsgrundlag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForaeldelseStatusDto {

  private UUID fordringId;
  private LocalDate currentFristExpires;
  private LocalDate udskydelseDato;

  @JsonProperty("isInUdskydelse")
  private Boolean isInUdskydelse;

  private Retsgrundlag retsgrundlag;
  private List<AfbrydelseHistoryEntryDto> afbrydelseHistory;
  private List<TillaegsfristHistoryEntryDto> tillaegsfristHistory;
  private ForaeldelseStatus status;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AfbrydelseHistoryEntryDto {
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
  public static class TillaegsfristHistoryEntryDto {
    private String type;
    private LocalDate appliedDate;
    private Integer extensionYears;
    private LocalDate newFristExpires;
    private String legalReference;
  }
}
