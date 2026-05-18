package dk.ufst.opendebt.debtservice.limitation.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import dk.ufst.opendebt.debtservice.limitation.client.LimitationObjectionWorkflowClient;
import dk.ufst.opendebt.debtservice.limitation.client.dto.CreateObjectionWorkflowRequest;
import dk.ufst.opendebt.debtservice.limitation.client.dto.ObjectionDecisionRequest;
import dk.ufst.opendebt.debtservice.limitation.client.dto.ObjectionWorkflowResult;
import dk.ufst.opendebt.debtservice.limitation.dto.EvaluateObjectionRequest;
import dk.ufst.opendebt.debtservice.limitation.dto.ForaeldelseStatusDto;
import dk.ufst.opendebt.debtservice.limitation.dto.ObjectionRegistrationResult;
import dk.ufst.opendebt.debtservice.limitation.dto.RegisterObjectionRequest;
import dk.ufst.opendebt.debtservice.limitation.entity.ForaeldelseStatus;
import dk.ufst.opendebt.debtservice.limitation.entity.ObjectionStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LimitationObjectionFacade {

  private final LimitationStateApplicationService applicationService;
  private final LimitationObjectionWorkflowClient workflowClient;
  private final LimitationAuditPublisher auditPublisher;

  public ObjectionRegistrationResult registerObjection(
      UUID fordringId, RegisterObjectionRequest request) {
    rejectUnexpectedPublicFields(request.hasUnexpectedFields());
    ObjectionWorkflowResult result =
        workflowClient.createWorkflow(
            CreateObjectionWorkflowRequest.builder()
                .fordringId(fordringId)
                .debtorPersonId(applicationService.getDebtorPersonId(fordringId))
                .registeredBy(currentActor())
                .sourceSurface("LIMITATION_SURFACE")
                .sourceReference("/api/v1/foraeldelse/" + fordringId + "/indsigelse")
                .build());
    applicationService.markObjectionPending(
        fordringId, result.getIndsigelsesId(), result.getWorkflowCaseId());
    auditPublisher.publishObjectionRegistered(fordringId, result.getIndsigelsesId());
    return ObjectionRegistrationResult.builder()
        .indsigelsesId(result.getIndsigelsesId())
        .status(ForaeldelseStatus.INDSIGELSE_PENDING)
        .build();
  }

  public ForaeldelseStatusDto evaluateObjection(
      UUID fordringId, UUID indsigelsesId, EvaluateObjectionRequest request) {
    rejectUnexpectedPublicFields(request.hasUnexpectedFields());
    applicationService.getStatus(fordringId);
    workflowClient.recordDecision(
        indsigelsesId,
        ObjectionDecisionRequest.builder()
            .outcome(request.getOutcome())
            .rationale(request.getRationale())
            .decidedBy(currentActor())
            .build());
    ObjectionStatus nextStatus =
        "VALID".equalsIgnoreCase(request.getOutcome())
            ? ObjectionStatus.FORAELDET
            : ObjectionStatus.ACTIVE;
    ForaeldelseStatusDto response =
        applicationService.resolveObjection(
            fordringId, indsigelsesId, nextStatus, request.getRationale());
    auditPublisher.publishObjectionEvaluated(fordringId, indsigelsesId, request.getOutcome());
    return response;
  }

  private void rejectUnexpectedPublicFields(boolean hasUnexpectedFields) {
    if (hasUnexpectedFields) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Public objection commands must not accept identity fields");
    }
  }

  private String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || authentication.getName() == null
        || authentication.getName().isBlank()) {
      return "system";
    }
    return authentication.getName();
  }
}
