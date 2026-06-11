package dk.ufst.opendebt.debtservice.service;

import java.util.List;

import org.springframework.stereotype.Component;

import dk.ufst.opendebt.debtservice.entity.ModregningEvent;
import dk.ufst.opendebt.debtservice.repository.ModregningEventRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ModregningResultMapper {

  private final ModregningEventRepository modregningEventRepository;

  public ModregningResult toResult(ModregningEvent event, List<FordringCoverageDto> coverages) {
    String supersedesDecisionReference =
        event.getSupersedesEventId() == null
            ? null
            : modregningEventRepository
                .findById(event.getSupersedesEventId())
                .map(ModregningEvent::getDecisionReference)
                .orElse(null);
    boolean hasHistory =
        modregningEventRepository.countByLineageReference(event.getLineageReference()) > 1;
    return new ModregningResult(
        event.getDecisionReference(),
        event.getLineageReference(),
        event.getDecisionKind(),
        event.isOperative(),
        supersedesDecisionReference,
        hasHistory,
        event.getId(),
        event.getDebtorPersonId(),
        event.getDecisionDate(),
        event.getDisbursementAmount(),
        event.getTier1Amount(),
        event.getTier2Amount(),
        event.getTier3Amount(),
        event.getResidualPayoutAmount(),
        event.isTier2WaiverApplied(),
        event.isNoticeDelivered(),
        event.getNoticeDeliveryDate(),
        event.getKlageFristDato(),
        event.getRenteGodtgoerelseStartDate(),
        event.isRenteGodtgoerelseNonTaxable(),
        coverages);
  }
}
