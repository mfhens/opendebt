package dk.ufst.opendebt.caseservice.workflow;

import java.util.Map;
import java.util.UUID;

public interface CaseWorkflowService {

  /** Starts a new debt collection workflow for a case. */
  String startCaseWorkflow(UUID caseId, String collectionStrategy, String assignedCaseworker);

  /** Gets the current workflow status for a case. */
  WorkflowStatus getWorkflowStatus(UUID caseId);

  /** Completes a user task in the workflow. */
  void completeTask(String taskId, Map<String, Object> variables);

  /** Claims a task for a specific user. */
  void claimTask(String taskId, String userId);

  /** Signals an event to the workflow (e.g., payment received, appeal filed). */
  void signalEvent(UUID caseId, String signalName, Map<String, Object> variables);

  /** Cancels a running workflow. */
  void cancelWorkflow(UUID caseId, String reason);

  record WorkflowStatus(
      String processInstanceId,
      String currentActivity,
      String status,
      Map<String, Object> variables) {}
}
