Feature: OpenTelemetry-based observability across OpenDebt services

  Background:
    Given all OpenDebt services are instrumented with OpenTelemetry
    And an OpenTelemetry Collector is deployed and receiving OTLP data

  Scenario: Structured JSON logs include traceId and spanId
    Given a service receives an HTTP request with an active trace context
    When the service writes a log entry during request processing
    Then the log entry is in JSON format
    And the log entry contains a "traceId" field with a non-empty value
    And the log entry contains a "spanId" field with a non-empty value

  Scenario: Distributed trace spans a multi-service call chain
    Given a request enters creditor-portal and triggers calls to debt-service and rules-engine
    When the request completes
    Then a single distributed trace is recorded in the trace backend
    And the trace contains spans for creditor-portal, debt-service, and rules-engine
    And all spans share the same traceId

  Scenario: W3C Trace Context headers are propagated on inter-service REST calls
    Given service A makes a REST call to service B
    When the outgoing request is sent
    Then the request includes a "traceparent" header conforming to W3C Trace Context
    And the span created by service B is a child of the span from service A

  Scenario: JVM metrics are exported for each service
    Given a service is running and exporting metrics
    When an operator queries the metrics backend for that service
    Then JVM heap usage metrics are available
    And JVM garbage collection metrics are available
    And JVM thread count metrics are available

  Scenario: HTTP server metrics are exported for each service
    Given a service has processed HTTP requests
    When an operator queries the metrics backend for that service
    Then HTTP request count metrics are available
    And HTTP request latency histogram metrics are available
    And HTTP error rate metrics are available

  Scenario: Trace lookup by traceId in the trace visualization tool
    Given a distributed trace has been recorded with traceId "abc123def456"
    When an operator searches for traceId "abc123def456" in the trace visualization tool
    Then the full call chain is displayed with service names and durations

  Scenario: Log correlation by traceId in the log aggregation tool
    Given multiple services have written log entries for a request with traceId "abc123def456"
    When an operator filters logs by traceId "abc123def456" in the log aggregation tool
    Then log entries from all involved services are returned
    And the entries can be sorted chronologically

  Scenario: Custom business metric is registered and queryable
    Given a service registers a custom counter metric "fordring_submissions_total"
    And the service increments the counter when a fordring is submitted
    When an operator queries the metrics backend for "fordring_submissions_total"
    Then the metric value reflects the number of submissions

  Scenario: PII is not present in trace spans or log output
    Given a request processes data for a person identified by person_id UUID
    When trace spans and log entries are recorded for the request
    Then no span attribute contains a CPR number, CVR number, name, or address
    And no log entry contains a CPR number, CVR number, name, or address
    And person references use only the person_id UUID

  Scenario: Local development observability stack runs via Docker Compose
    Given the developer runs the Docker Compose configuration
    When the observability stack containers start
    Then the OpenTelemetry Collector is reachable
    And the trace visualization UI is accessible
    And the log aggregation UI is accessible
    And the metrics dashboard UI is accessible

  Scenario: Production observability stack is defined as Kubernetes manifests
    Given Kustomize overlays exist for the observability stack
    When the overlays are applied to a Kubernetes cluster
    Then the OpenTelemetry Collector is deployed
    And the trace, log, and metrics backends are deployed and reachable

  Scenario: Trace instrumentation overhead is within acceptable limits
    Given a service endpoint has a baseline p99 latency measured without tracing
    When tracing is enabled and the same endpoint is measured under normal load
    Then the p99 latency increase is no more than 5 milliseconds
