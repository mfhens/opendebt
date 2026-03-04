package dk.ufst.opendebt.caseservice.workflow.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.caseservice.workflow.CaseWorkflowService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseWorkflowServiceImpl implements CaseWorkflowService {

  private static final String PROCESS_DEFINITION_KEY = "debtCollectionCase";
  private static final String CASE_ID_VAR = "caseId";

  private final RuntimeService runtimeService;
  private final TaskService taskService;
  private final HistoryService historyService;

  @Override
  @Transactional
  public String startCaseWorkflow(
      UUID caseId, String collectionStrategy, String assignedCaseworker) {
    log.info("Starting workflow for case: {}, strategy: {}", caseId, collectionStrategy);

    Map<String, Object> variables = new HashMap<>();
    variables.put(CASE_ID_VAR, caseId.toString());
    variables.put("collectionStrategy", collectionStrategy);
    variables.put("assignedCaseworker", assignedCaseworker);

    ProcessInstance processInstance =
        runtimeService.startProcessInstanceByKey(
            PROCESS_DEFINITION_KEY, caseId.toString(), variables);

    log.info("Started workflow instance: {} for case: {}", processInstance.getId(), caseId);
    return processInstance.getId();
  }

  @Override
  public WorkflowStatus getWorkflowStatus(UUID caseId) {
    ProcessInstance instance =
        runtimeService
            .createProcessInstanceQuery()
            .processInstanceBusinessKey(caseId.toString())
            .singleResult();

    if (instance != null) {
      Execution execution =
          runtimeService
              .createExecutionQuery()
              .processInstanceId(instance.getId())
              .onlyChildExecutions()
              .singleResult();

      String currentActivity = execution != null ? execution.getActivityId() : "unknown";
      Map<String, Object> variables = runtimeService.getVariables(instance.getId());

      return new WorkflowStatus(instance.getId(), currentActivity, "ACTIVE", variables);
    }

    // Check history for completed processes
    HistoricProcessInstance historicInstance =
        historyService
            .createHistoricProcessInstanceQuery()
            .processInstanceBusinessKey(caseId.toString())
            .singleResult();

    if (historicInstance != null) {
      return new WorkflowStatus(
          historicInstance.getId(),
          "completed",
          historicInstance.getEndTime() != null ? "COMPLETED" : "UNKNOWN",
          Map.of());
    }

    return null;
  }

  @Override
  @Transactional
  public void completeTask(String taskId, Map<String, Object> variables) {
    log.info("Completing task: {} with variables: {}", taskId, variables.keySet());
    taskService.complete(taskId, variables);
  }

  @Override
  @Transactional
  public void claimTask(String taskId, String userId) {
    log.info("User {} claiming task: {}", userId, taskId);
    taskService.claim(taskId, userId);
  }

  @Override
  @Transactional
  public void signalEvent(UUID caseId, String signalName, Map<String, Object> variables) {
    log.info("Signaling event {} for case: {}", signalName, caseId);

    ProcessInstance instance =
        runtimeService
            .createProcessInstanceQuery()
            .processInstanceBusinessKey(caseId.toString())
            .singleResult();

    if (instance != null) {
      runtimeService.signalEventReceived(signalName, instance.getId(), variables);
    } else {
      log.warn("No active workflow found for case: {}", caseId);
    }
  }

  @Override
  @Transactional
  public void cancelWorkflow(UUID caseId, String reason) {
    log.info("Cancelling workflow for case: {}, reason: {}", caseId, reason);

    ProcessInstance instance =
        runtimeService
            .createProcessInstanceQuery()
            .processInstanceBusinessKey(caseId.toString())
            .singleResult();

    if (instance != null) {
      runtimeService.deleteProcessInstance(instance.getId(), reason);
      log.info("Workflow cancelled for case: {}", caseId);
    }
  }
}
