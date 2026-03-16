package dk.ufst.opendebt.debtservice.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.debtservice.dto.HoeringInfoDto;
import dk.ufst.opendebt.debtservice.dto.KvitteringResponse;
import dk.ufst.opendebt.debtservice.dto.SlutstatusEnum;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringEntity;
import dk.ufst.opendebt.debtservice.service.KvitteringService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KvitteringServiceImpl implements KvitteringService {

  @Override
  public KvitteringResponse buildKvittering(
      UUID debtId, DebtEntity entity, List<String> validationErrors, HoeringEntity hoering) {

    if (validationErrors != null && !validationErrors.isEmpty()) {
      log.info(
          "Building AFVIST kvittering for debt {}, errors: {}", debtId, validationErrors.size());
      return KvitteringResponse.builder()
          .fordringsId(debtId)
          .slutstatus(SlutstatusEnum.AFVIST)
          .afvistBegrundelse(validationErrors.get(0))
          .build();
    }

    if (hoering != null) {
      log.info("Building HOERING kvittering for debt {}, hearing: {}", debtId, hoering.getId());
      HoeringInfoDto hoeringInfo =
          HoeringInfoDto.builder()
              .hoeringId(hoering.getId())
              .deviationDescription(hoering.getDeviationDescription())
              .slaDeadline(hoering.getSlaDeadline())
              .build();
      return KvitteringResponse.builder()
          .fordringsId(debtId)
          .slutstatus(SlutstatusEnum.HOERING)
          .hoeringInfo(hoeringInfo)
          .build();
    }

    log.info("Building UDFOERT kvittering for debt {}", debtId);
    return KvitteringResponse.builder()
        .fordringsId(debtId)
        .slutstatus(SlutstatusEnum.UDFOERT)
        .build();
  }
}
