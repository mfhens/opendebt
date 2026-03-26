package dk.ufst.opendebt.gateway.soap.oio;

import org.springframework.stereotype.Component;

import dk.ufst.opendebt.common.dto.soap.*;
import dk.ufst.opendebt.gateway.soap.ClaimMapperUtils;
import dk.ufst.opendebt.gateway.soap.oio.generated.*;

@Component
public class OioClaimMapper {

  public FordringSubmitRequest toSubmitRequest(MFFordringIndberet_IRequest req) {
    return FordringSubmitRequest.builder()
        .claimType(req.getFordringsType())
        .amount(req.getBeloeb())
        .debtorPersonId(req.getSkyldnerPersonId())
        .claimDate(ClaimMapperUtils.parseDate(req.getFordringsDato()))
        .dueDate(ClaimMapperUtils.parseDate(req.getForfaldsDato()))
        .externalId(req.getEksternId())
        .build();
  }

  public MFFordringIndberet_IResponse toSubmitResponse(ClaimSubmissionResponse resp) {
    MFFordringIndberet_IResponse r = new MFFordringIndberet_IResponse();
    r.setFordringsId(resp.getClaimId());
    r.setStatus(ClaimMapperUtils.mapOutcome(resp.getOutcome()));
    return r;
  }

  public String toClaimId(MFKvitteringHent_IRequest req) {
    return req.getFordringsId();
  }

  public MFKvitteringHent_IResponse toReceiptResponse(KvitteringResponse kvittering) {
    MFKvitteringHent_IResponse r = new MFKvitteringHent_IResponse();
    r.setKvitteringId(kvittering.getKvitteringId());
    r.setFordringsId(kvittering.getClaimId());
    r.setStatus(kvittering.getStatus());
    if (kvittering.getModtagetDato() != null)
      r.setModtagetDato(kvittering.getModtagetDato().toString());
    if (kvittering.getBehandletDato() != null)
      r.setBehandletDato(kvittering.getBehandletDato().toString());
    r.setAfvisningKode(kvittering.getAfvisningKode());
    r.setAfvisningTekst(kvittering.getAfvisningTekst());
    return r;
  }

  public String toClaimId(MFUnderretSamlingHent_IRequest req) {
    return req.getFordringsId();
  }

  public String toDebtorId(MFUnderretSamlingHent_IRequest req) {
    return req.getSkyldnerPersonId();
  }

  public MFUnderretSamlingHent_IResponse toNotificationResponse(
      NotificationCollectionResult result) {
    MFUnderretSamlingHent_IResponse r = new MFUnderretSamlingHent_IResponse();
    r.setFordringsId(result.getClaimId());
    r.setTotal(result.getTotal());
    MFUnderretSamlingHent_IResponse.Underretninger underretninger =
        new MFUnderretSamlingHent_IResponse.Underretninger();
    if (result.getNotifications() != null) {
      for (NotificationDto dto : result.getNotifications()) {
        MFUnderretSamlingHent_IResponse.Underretning u =
            new MFUnderretSamlingHent_IResponse.Underretning();
        u.setUnderretningId(dto.getNotificationId());
        u.setType(dto.getType());
        u.setStatus(dto.getStatus());
        if (dto.getCreatedAt() != null) u.setOprettetDato(dto.getCreatedAt().toString());
        u.setBeskrivelse(dto.getDescription());
        underretninger.getUnderretning().add(u);
      }
    }
    r.setUnderretninger(underretninger);
    return r;
  }
}
