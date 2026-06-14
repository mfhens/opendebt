package dk.ufst.opendebt.debtservice.attachment;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttachmentWorkflowApplicationService implements AttachmentWorkflowApi {

  private final Map<UUID, List<AttachmentWorkflowDto>> workflowsByDebtor = new LinkedHashMap<>();

  @Override
  public AttachmentWorkflowDto createWorkflow(UUID debtorId, CreateAttachmentWorkflowRequest request) {
    if (request == null || request.getCoveredFordringIds() == null || request.getCoveredFordringIds().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "coveredFordringIds are required");
    }
    UUID workflowId = UUID.randomUUID();
    String workflowReference = "WR-" + workflowId;
    AttachmentWorkflowDto workflow =
        AttachmentWorkflowDto.builder()
            .workflowId(workflowId)
            .debtorId(debtorId)
            .coveredFordringIds(List.copyOf(request.getCoveredFordringIds()))
            .status(AttachmentWorkflowStatus.REQUESTED)
            .workflowReference(workflowReference)
            .outcomeQualifier(AttachmentWorkflowOutcomeQualifier.NONE)
            .dispatchMetadata(
                AttachmentWorkflowDispatchMetadata.builder().workflowReference(workflowReference).build())
            .history(new ArrayList<>())
            .build();
    workflow
        .getHistory()
        .add(
            AttachmentWorkflowHistoryEntry.builder()
                .status(AttachmentWorkflowStatus.REQUESTED)
                .recordedAt(OffsetDateTime.now())
                .note("workflow created")
                .build());
    workflowsByDebtor.computeIfAbsent(debtorId, ignored -> new ArrayList<>()).add(workflow);
    return workflow;
  }

  @Override
  public AttachmentWorkflowDto dispatchWorkflow(UUID debtorId, UUID workflowId) {
    AttachmentWorkflowDto workflow = requireWorkflow(debtorId, workflowId);
    if (workflow.getStatus() == AttachmentWorkflowStatus.REQUESTED) {
      workflow.setStatus(AttachmentWorkflowStatus.IN_COURT_PROCESS);
      workflow
          .getHistory()
          .add(
              AttachmentWorkflowHistoryEntry.builder()
                  .status(AttachmentWorkflowStatus.IN_COURT_PROCESS)
                  .recordedAt(OffsetDateTime.now())
                  .note("dispatch accepted")
                  .build());
    }
    return workflow;
  }

  @Override
  public AttachmentWorkflowDto withdrawWorkflow(
      UUID debtorId, UUID workflowId, WithdrawAttachmentWorkflowRequest request) {
    AttachmentWorkflowDto workflow = requireWorkflow(debtorId, workflowId);
    if (request == null || request.getReason() == null || request.getReason().isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "withdraw reason is required");
    }
    workflow.setStatus(AttachmentWorkflowStatus.WITHDRAWN);
    workflow.setOutcomeQualifier(AttachmentWorkflowOutcomeQualifier.WITHDRAWN);
    workflow
        .getHistory()
        .add(
            AttachmentWorkflowHistoryEntry.builder()
                .status(AttachmentWorkflowStatus.WITHDRAWN)
                .recordedAt(OffsetDateTime.now())
                .note(request.getReason())
                .build());
    return workflow;
  }

  @Override
  public AttachmentWorkflowDto processCallback(UUID debtorId, AttachmentWorkflowCallbackRequest request) {
    if (request == null || request.getWorkflowReference() == null || request.getWorkflowReference().isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "workflowReference is required");
    }
    AttachmentWorkflowDto workflow =
        workflowsByDebtor.getOrDefault(debtorId, List.of()).stream()
            .filter(candidate -> request.getWorkflowReference().equals(candidate.getWorkflowReference()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown workflowReference"));
    AttachmentWorkflowStatus newStatus = AttachmentWorkflowStatus.valueOf(request.getStatus());
    workflow.setStatus(newStatus);
    workflow
        .getHistory()
        .add(
            AttachmentWorkflowHistoryEntry.builder()
                .status(newStatus)
                .recordedAt(OffsetDateTime.now())
                .note(request.getReasonCode())
                .outcomeDate(request.getOutcomeDate())
                .build());
    if (newStatus == AttachmentWorkflowStatus.UNSUCCESSFUL && request.getReasonCode() != null) {
      workflow.setOutcomeQualifier(AttachmentWorkflowOutcomeQualifier.valueOf(request.getReasonCode()));
    } else if (newStatus == AttachmentWorkflowStatus.COMPLETED) {
      workflow.setOutcomeQualifier(AttachmentWorkflowOutcomeQualifier.COMPLETED);
    }
    workflow.setInterruptionLinkageMetadata(
        request.getOutcomeDate() == null ? null : "UDLAEG@" + request.getOutcomeDate());
    return workflow;
  }

  @Override
  public List<AttachmentWorkflowDto> getWorkflows(UUID debtorId) {
    return List.copyOf(workflowsByDebtor.getOrDefault(debtorId, List.of()));
  }

  @Override
  public AttachmentWorkflowDto getWorkflow(UUID debtorId, UUID workflowId) {
    return requireWorkflow(debtorId, workflowId);
  }

  private AttachmentWorkflowDto requireWorkflow(UUID debtorId, UUID workflowId) {
    return workflowsByDebtor.getOrDefault(debtorId, List.of()).stream()
        .filter(workflow -> workflow.getWorkflowId().equals(workflowId))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown workflowId"));
  }
}
