package dk.ufst.opendebt.caseservice.limitation;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import dk.ufst.opendebt.caseservice.limitation.dto.CreateLimitationObjectionWorkflowRequest;
import dk.ufst.opendebt.caseservice.limitation.dto.LimitationObjectionDecisionRequest;
import dk.ufst.opendebt.caseservice.limitation.dto.LimitationObjectionWorkflowResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LimitationObjectionWorkflowServiceImpl implements LimitationObjectionWorkflowService {

  private final LimitationObjectionWorkflowRepository repository;

  @Override
  public LimitationObjectionWorkflowResult createWorkflow(
      CreateLimitationObjectionWorkflowRequest request) {
    UUID id = UUID.randomUUID();
    UUID workflowCaseId = UUID.randomUUID();
    repository.save(
        LimitationObjectionWorkflowRecord.builder()
            .id(id)
            .fordringId(request.getFordringId())
            .debtorPersonId(request.getDebtorPersonId())
            .workflowCaseId(workflowCaseId)
            .status("REGISTERED")
            .registeredBy(request.getRegisteredBy())
            .registeredAt(Instant.now())
            .build());
    return LimitationObjectionWorkflowResult.builder()
        .indsigelsesId(id)
        .workflowCaseId(workflowCaseId)
        .workflowStatus("REGISTERED")
        .build();
  }

  @Override
  public LimitationObjectionWorkflowResult recordDecision(
      UUID indsigelsesId, LimitationObjectionDecisionRequest request) {
    LimitationObjectionWorkflowRecord record =
        repository
            .findById(indsigelsesId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown indsigelsesId"));
    record.setStatus(request.getOutcome());
    record.setRationale(request.getRationale());
    record.setDecidedBy(request.getDecidedBy());
    record.setDecidedAt(Instant.now());
    repository.save(record);
    return LimitationObjectionWorkflowResult.builder()
        .indsigelsesId(record.getId())
        .workflowCaseId(record.getWorkflowCaseId())
        .workflowStatus(record.getStatus())
        .authoritativeOutcome(record.getStatus())
        .decidedAt(record.getDecidedAt())
        .build();
  }
}
