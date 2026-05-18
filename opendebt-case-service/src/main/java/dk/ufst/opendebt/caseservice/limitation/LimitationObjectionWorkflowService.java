package dk.ufst.opendebt.caseservice.limitation;

import java.util.UUID;

import dk.ufst.opendebt.caseservice.limitation.dto.CreateLimitationObjectionWorkflowRequest;
import dk.ufst.opendebt.caseservice.limitation.dto.LimitationObjectionDecisionRequest;
import dk.ufst.opendebt.caseservice.limitation.dto.LimitationObjectionWorkflowResult;

public interface LimitationObjectionWorkflowService {

  LimitationObjectionWorkflowResult createWorkflow(
      CreateLimitationObjectionWorkflowRequest request);

  LimitationObjectionWorkflowResult recordDecision(
      UUID indsigelsesId, LimitationObjectionDecisionRequest request);
}
