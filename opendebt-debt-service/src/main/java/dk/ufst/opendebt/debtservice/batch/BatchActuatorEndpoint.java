package dk.ufst.opendebt.debtservice.batch;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom Spring Boot Actuator endpoint that allows manual triggering of the OpenDebt batch jobs.
 *
 * <p>Exposed at: POST /debt-service/actuator/batch/{job}
 *
 * <p>Jobs: interest-accrual — calculates daily interest for all OVERDRAGET debts
 * restance-transition — transitions eligible debts to RESTANCE lifecycle state deadline-monitoring
 * — checks approaching limitation/payment deadlines
 *
 * <p>Usage: curl -X POST http://localhost:8082/debt-service/actuator/batch/interest-accrual curl -X
 * POST "http://localhost:8082/debt-service/actuator/batch/interest-accrual?date=2026-03-20"
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Endpoint(id = "batch")
public class BatchActuatorEndpoint {

  private final InterestAccrualJob interestAccrualJob;
  private final RestanceTransitionJob restanceTransitionJob;
  private final DeadlineMonitoringJob deadlineMonitoringJob;

  /**
   * Triggers a batch job by name.
   *
   * @param job One of: interest-accrual, restance-transition, deadline-monitoring
   * @param date Optional ISO date (yyyy-MM-dd). Defaults to today.
   */
  @WriteOperation
  public Map<String, Object> trigger(String job, @Nullable String date) {
    LocalDate runDate = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();

    log.info("Manual batch trigger: job={}, date={}", job, runDate);

    return switch (job) {
      case "interest-accrual" -> run(interestAccrualJob.execute(runDate), job, runDate);
      case "restance-transition" -> run(restanceTransitionJob.execute(runDate), job, runDate);
      case "deadline-monitoring" -> run(deadlineMonitoringJob.execute(runDate), job, runDate);
      default -> {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error", "Unknown job: " + job);
        err.put(
            "availableJobs",
            new String[] {"interest-accrual", "restance-transition", "deadline-monitoring"});
        yield err;
      }
    };
  }

  private Map<String, Object> run(BatchJobExecutionEntity result, String job, LocalDate date) {
    Map<String, Object> response = new LinkedHashMap<>();
    if (result == null) {
      response.put("status", "SKIPPED");
      response.put("reason", "Job already executed for " + date);
      response.put("job", job);
      response.put("date", date.toString());
    } else {
      response.put("status", result.getStatus().name());
      response.put("job", job);
      response.put("date", date.toString());
      response.put("recordsProcessed", result.getRecordsProcessed());
      response.put("recordsFailed", result.getRecordsFailed());
      response.put("executionId", result.getId().toString());
    }
    return response;
  }
}
