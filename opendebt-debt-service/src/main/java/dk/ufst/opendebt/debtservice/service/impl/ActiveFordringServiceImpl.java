package dk.ufst.opendebt.debtservice.service.impl;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.debtservice.dto.ActiveFordringResponseDto;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.CollectionMeasureRepository;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.ActiveFordringService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves the ordered list of active fordringer for a debtor.
 *
 * <p>Query strategy:
 *
 * <ol>
 *   <li>Fetch all active debts in a single query (positive balance, non-terminal status), ordered
 *       by sekvensNummer / receivedAt.
 *   <li>Resolve {@code inLoenindeholdelsesIndsats} for the whole batch in one extra query (avoids
 *       N+1).
 *   <li>Map to {@link ActiveFordringResponseDto}.
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActiveFordringServiceImpl implements ActiveFordringService {

  private final DebtRepository debtRepository;
  private final CollectionMeasureRepository collectionMeasureRepository;

  @Override
  @Transactional(readOnly = true)
  public List<ActiveFordringResponseDto> getActiveFordringer(UUID debtorPersonId) {
    log.debug("Resolving active fordringer for debtor {}", debtorPersonId);

    List<DebtEntity> activeDebts =
        debtRepository.findActiveFordringerByDebtorPersonId(debtorPersonId);

    if (activeDebts.isEmpty()) {
      return Collections.emptyList();
    }

    // Batch resolve lønindeholdelse status — one query for all debts, not N
    Set<UUID> loenDebtIds =
        collectionMeasureRepository.findActiveWageGarnishmentDebtIds(
            activeDebts.stream().map(DebtEntity::getId).collect(Collectors.toSet()));

    return activeDebts.stream()
        .map(debt -> toDto(debt, loenDebtIds.contains(debt.getId())))
        .collect(Collectors.toList());
  }

  private ActiveFordringResponseDto toDto(DebtEntity d, boolean inLoen) {
    return ActiveFordringResponseDto.builder()
        .fordringId(d.getId())
        .fordringType(d.getDebtTypeCode())
        .beloebResterende(d.getOutstandingBalance())
        .opkraevningsrenter(d.getBeloebOpkraevningsrenter())
        .inddrivelsesrenterFordringshaver(d.getBeloebInddrivelsesrenterFordringshaver())
        .inddrivelsesrenterFoerTilbagefoersel(d.getBeloebInddrivelsesrenterFoerTilbagefoersel())
        .inddrivelsesrenterStk1(d.getBeloebInddrivelsesrenterStk1())
        .oevrigeRenterPsrm(d.getBeloebOevrigeRenterPsrm())
        .inddrivelsesomkostninger(d.getFeesAmount())
        .sekvensNummer(d.getSekvensNummer())
        .inLoenindeholdelsesIndsats(inLoen)
        .opskrivningAfFordringId(d.getParentClaimId())
        .fordringshaverId(d.getCreditorOrgId())
        .gilParagraf(d.getGilParagraf())
        .applicationTimestamp(
            d.getReceivedAt() != null ? d.getReceivedAt().atOffset(ZoneOffset.UTC) : null)
        .build();
  }
}
