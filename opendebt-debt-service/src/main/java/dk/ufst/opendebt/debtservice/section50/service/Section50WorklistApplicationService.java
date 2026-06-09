package dk.ufst.opendebt.debtservice.section50.service;

import java.util.UUID;

import dk.ufst.opendebt.debtservice.section50.dto.GenerateSection50WorklistRequest;
import dk.ufst.opendebt.debtservice.section50.dto.Section50ModregningDecisionRequest;
import dk.ufst.opendebt.debtservice.section50.dto.Section50OverrideRequest;
import dk.ufst.opendebt.debtservice.section50.dto.Section50WorklistDto;

public interface Section50WorklistApplicationService {
  Section50WorklistDto generateWorklist(
      UUID debtorPersonId, GenerateSection50WorklistRequest request);

  Section50WorklistDto getWorklist(UUID debtorPersonId, UUID worklistId);

  Section50WorklistDto applyOverride(
      UUID debtorPersonId, UUID worklistId, Section50OverrideRequest request);

  Section50WorklistDto recordModregningDecision(
      UUID debtorPersonId, UUID worklistId, Section50ModregningDecisionRequest request);
}
