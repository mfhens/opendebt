package dk.ufst.opendebt.creditor.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import dk.ufst.opendebt.creditor.dto.FordringFormDto;
import dk.ufst.opendebt.creditor.dto.PortalDebtDto;

/**
 * Maps a {@link FordringFormDto} to the {@link PortalDebtDto} payload expected by
 * DebtServiceClient.
 */
@Component
public class FordringMapper {

  /**
   * Converts the form DTO and the acting creditor's org ID into a debt-service creation request.
   *
   * @param form the validated form data
   * @param creditorOrgId the acting creditor organisation ID
   * @return a {@link PortalDebtDto} ready to be sent to the debt service
   */
  public PortalDebtDto toDebtRequest(FordringFormDto form, UUID creditorOrgId) {
    return PortalDebtDto.builder()
        .debtorPersonId(form.getDebtorPersonId())
        .creditorOrgId(creditorOrgId)
        .principalAmount(form.getPrincipalAmount())
        .dueDate(form.getDueDate())
        .debtTypeCode(form.getDebtTypeCode())
        .description(form.getDescription())
        .build();
  }
}
