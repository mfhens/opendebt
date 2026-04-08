# ADR 0016: Flowable for Workflow Engine

## Status
Accepted

## Context
Case management in debt collection follows a complex workflow:

1. Case created when debt(s) ready for collection
2. Initial assessment determines collection strategy
3. Execute strategy (voluntary payment → offsetting → wage garnishment)
4. Handle payments, appeals, escalations
5. Close case when debt cleared or written off

Requirements:
- Visual workflow modeling (BPMN 2.0)
- Long-running processes (months to years)
- Human tasks for caseworker intervention
- Timer events (payment deadlines, reminders)
- Event handling (appeals, payments received)
- Audit trail of workflow execution

## Decision
We adopt **Flowable 7.x** as the workflow/BPM engine, embedded in the case-service.

### Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                     CASE SERVICE                             │
│                      (Port 8081)                             │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Flowable Process Engine                  │  │
│  │  ┌────────────────────────────────────────────────┐  │  │
│  │  │        debt-collection-case.bpmn20.xml         │  │  │
│  │  │                                                │  │  │
│  │  │  [Start] → [Assess] → [Strategy Gateway]      │  │  │
│  │  │                ↓              ↓       ↓        │  │  │
│  │  │          [Voluntary]  [Offsetting] [Garnish]  │  │  │
│  │  │                ↓              ↓       ↓        │  │  │
│  │  │              [Payment Check] → [Close Case]   │  │  │
│  │  └────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
│                          │                                  │
│         Service Tasks (Delegates)                           │
│    - CaseAssessmentDelegate                                │
│    - SendLetterDelegate                                    │
│    - CheckPaymentDelegate                                  │
│    - CloseCaseDelegate                                     │
└─────────────────────────────────────────────────────────────┘
```

### Workflow Overview

```
┌─────────┐    ┌──────────┐    ┌─────────────────┐
│  Start  │───►│  Assess  │───►│ Strategy Gateway│
└─────────┘    └──────────┘    └────────┬────────┘
                                        │
              ┌─────────────────────────┼─────────────────────────┐
              ▼                         ▼                         ▼
    ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
    │    Voluntary    │      │   Offsetting    │      │ Wage Garnishment│
    │    Payment      │      │   (Modregning)  │      │(Loenindeholdelse)│
    ├─────────────────┤      ├─────────────────┤      ├─────────────────┤
    │ Send Reminder   │      │ Initiate Offset │      │ Send Notice     │
    │ Wait 30 days    │      │ Send Notice     │      │ Monitor Payments│
    │ Check Payment   │      │ Wait 14 days    │      │ (User Task)     │
    └────────┬────────┘      └────────┬────────┘      └────────┬────────┘
             │                        │                        │
             └────────────────────────┴────────────────────────┘
                                      │
                               ┌──────┴──────┐
                               │  Paid?      │
                               └──────┬──────┘
                        ┌─────────────┴─────────────┐
                        ▼                           ▼
                  ┌───────────┐              ┌─────────────┐
                  │Close Case │              │  Escalate   │
                  └───────────┘              │  (Supervisor)│
                                             └─────────────┘
```

### Key Features Used

| Feature | Usage |
|---------|-------|
| Service Tasks | Automated actions (send letter, check payment) |
| User Tasks | Manual caseworker/supervisor actions |
| Timer Events | Payment deadlines, reminder scheduling |
| Signal Events | Appeal received, payment confirmed |
| Exclusive Gateway | Decision points (paid? strategy?) |
| Boundary Events | Handle appeals during garnishment |

### Database
Flowable uses its own tables (ACT_*) in the case-service database:
- `ACT_RU_*` - Runtime data
- `ACT_HI_*` - History/audit data
- `ACT_ID_*` - Identity management

### API Integration
```java
// Start workflow
workflowService.startCaseWorkflow(caseId, "VOLUNTARY_PAYMENT", "caseworker1");

// Complete user task
workflowService.completeTask(taskId, Map.of("approved", true));

// Signal event
workflowService.signalEvent(caseId, "paymentReceived", Map.of("amount", 5000));
```

## Consequences

### Positive
- **Visual modeling**: BPMN diagrams for business understanding
- **Long-running**: Handles processes spanning months/years
- **Audit trail**: Full history of workflow execution
- **Human tasks**: Built-in task management for caseworkers
- **Open source**: Apache 2.0 license, active community
- **Spring Boot**: First-class integration

### Negative
- **Database overhead**: Additional tables for workflow state
- **Complexity**: BPMN learning curve
- **Debugging**: Harder to debug than simple code

### Mitigations
- Use Flowable Modeler for visual editing
- Comprehensive logging in delegates
- Unit test workflows with Flowable test support

## Alternatives Considered

| Option | Reason Not Chosen |
|--------|-------------------|
| Camunda 8 | Cloud-native but more complex to self-host |
| Spring State Machine | No visual modeling, less suited for long-running |
| Temporal | Overkill for our use case, less BPMN focus |
| jBPM | Less active development than Flowable |
