package dk.ufst.opendebt.common.dto;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CasePartyDto {

  private UUID id;
  private UUID caseId;
  private UUID personId;
  private String partyRole;
  private String partyType;
  private LocalDate activeFrom;
  private LocalDate activeTo;
}
