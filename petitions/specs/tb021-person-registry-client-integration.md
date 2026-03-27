# Specification: TB-021 — Replace Portal PersonRegistryClient Stubs

| Field | Value |
|---|---|
| **Spec ID** | TB-021 |
| **Source** | Tech Backlog TB-021 |
| **Scope** | `opendebt-creditor-portal` · `opendebt-caseworker-portal` |
| **Status** | Approved for implementation |

---

## 0. Prerequisite: Fix PersonNotFoundException HTTP Status in `opendebt-person-registry`

**Why this is in scope for TB-021:** `PersonServiceImpl.getPersonById` throws `PersonNotFoundException` when a person is not found or is soft-deleted. `GlobalExceptionHandler` in `opendebt-common` has `@ExceptionHandler(Exception.class)` which intercepts everything — including `PersonNotFoundException` — returning HTTP 500. Neither `@ResponseStatus` nor a `@ControllerAdvice` referencing a package-private type resolves this. The fix is: (1) make `PersonNotFoundException` `public` so it is referenceable cross-package, and (2) register a more-specific `@ExceptionHandler` in the `controller` package that Spring's `ExceptionHandlerExceptionResolver` prefers over `GlobalExceptionHandler`.

### §0.1 — Single change to `PersonServiceImpl.java`

- **Package:** `dk.ufst.opendebt.personregistry.service.impl`
- **Change:** Change the access modifier of the inner class:

```java
// BEFORE
static final class PersonNotFoundException extends RuntimeException {

// AFTER
public static final class PersonNotFoundException extends RuntimeException {
```

- **No other changes to `PersonServiceImpl`** — no logic, no imports, no exception messages.

### §0.2 — New class: `PersonRegistryExceptionHandler`

- **File to create:** `opendebt-person-registry/src/main/java/dk/ufst/opendebt/personregistry/controller/PersonRegistryExceptionHandler.java`
- **Package:** `dk.ufst.opendebt.personregistry.controller`
- **Annotation:** `@RestControllerAdvice`
- **One method:** `@ExceptionHandler(PersonServiceImpl.PersonNotFoundException.class)` → returns `ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Person not found"))`
- **Required imports:** `PersonServiceImpl`, `HttpStatus`, `ResponseEntity`, `Map`, `RestControllerAdvice`, `ExceptionHandler`
- **Why it wins over `GlobalExceptionHandler`:** Spring's `ExceptionHandlerExceptionResolver` picks the most specific `@ExceptionHandler` match. `PersonServiceImpl.PersonNotFoundException` is more specific than `Exception.class`, so this handler wins even though `GlobalExceptionHandler` has `@ExceptionHandler(Exception.class)`.

```java
package dk.ufst.opendebt.personregistry.controller;

import dk.ufst.opendebt.personregistry.service.impl.PersonServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class PersonRegistryExceptionHandler {

    @ExceptionHandler(PersonServiceImpl.PersonNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePersonNotFound(PersonServiceImpl.PersonNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Person not found"));
    }
}
```

### §0.3 — Acceptance criteria

```gherkin
# AC-EH-1 — person not found maps to 404
Given a request to GET /api/v1/persons/{id} where the person does not exist
When the request is processed
Then the response status is 404 Not Found
And the response body is {"error":"Person not found"}

# AC-EH-2 — soft-deleted person maps to 404
Given a request to GET /api/v1/persons/{id} where the person is soft-deleted
When the request is processed
Then the response status is 404 Not Found
And the response body is {"error":"Person not found"}
# Note: 410 is deferred per petition023 OC-7; PersonServiceImpl throws the same PersonNotFoundException for both cases.
```

### §0.4 — Tests

- **Test class:** `PersonRegistryExceptionHandlerTest` in `opendebt-person-registry` test tree
- **Setup:** `MockMvcBuilders.standaloneSetup(personController).setControllerAdvice(new PersonRegistryExceptionHandler()).build()`
- **Mock:** `PersonService` to throw `PersonServiceImpl.PersonNotFoundException`
- **Assert:** HTTP 404 and `{"error":"Person not found"}` body

---

## 1. Context and Boundaries

This spec replaces the stub bodies in two existing `PersonRegistryClient` classes. The class signatures, constructor shapes, `@CircuitBreaker`/`@Retry` annotations, fallback method signatures, and field `ERR_PERSON_REGISTRY_UNAVAILABLE` are **already correct and must not be changed**. Only the bodies of the annotated public methods change. All `AIDEV-TODO` comments are removed as part of this work.

**No new classes are created in the application layer.** All request/response DTOs for HTTP calls are defined as package-private static inner records inside the respective client class (see §4 and §5 below).

---

## 2. External API Contract (consumer view)

Both portals call the same person-registry service. The base URL is already wired in each portal's `application.yml`:

```
opendebt.services.person-registry.url  →  ${PERSON_REGISTRY_URL:http://localhost:8083}
```

### 2.1 POST `/person-registry/api/v1/persons/lookup`

**Request body** (JSON):

```json
{
  "identifier":     "<10-digit CPR string>",
  "identifierType": "CPR",
  "role":           "PERSONAL"
}
```

**Success response** (HTTP 200, JSON):

```json
{
  "personId": "<uuid>",
  "created":  true | false,
  "role":     "PERSONAL"
}
```

Only `personId` (UUID) and `created` (boolean) are consumed by this implementation.

### 2.2 GET `/person-registry/api/v1/persons/{personId}`

**Success response** (HTTP 200, JSON):

```json
{
  "id":   "<uuid>",
  "name": "<string | null>"
}
```

**Error responses consumed:**

| HTTP status | Meaning for this client |
|---|---|
| 404 | Person not found (or soft-deleted) — returned after `@ResponseStatus(NOT_FOUND)` is applied to `PersonNotFoundException` (§0). Treat as "not found", return a safe result. Does **not** open the circuit breaker. |
| 410 | Reserved for future use (petition023 OC-7, deferred). Treat identically to 404 for now. Does **not** open the circuit breaker. |
| 4xx (other) | Client error; propagate as `WebClientResponseException`; circuit breaker ignores 4xx |
| 5xx | Infrastructure failure; let circuit breaker catch; fallback fires |

> **Prerequisite dependency:** The 404 row above is only correct after `@ResponseStatus(HttpStatus.NOT_FOUND)` is added to `PersonNotFoundException` (§0). Without it, `PersonNotFoundException` surfaces as HTTP 500 — indistinguishable from an infrastructure failure — and the circuit breaker would incorrectly open. The client implementation in §4 and §5 is written to the post-fix contract and must not be deployed without §0.

Only `id` (UUID) and `name` (String) are consumed.

### 2.3 POST `/person-registry/api/v1/organizations/lookup`

**Request body** (JSON):

```json
{
  "cvr": "<8-digit string>"
}
```

**Success response** (HTTP 200, JSON):

```json
{
  "organizationId": "<uuid>"
}
```

Only `organizationId` (UUID) is consumed.

---

## 3. Shared Name-Comparison Algorithm

Used only in `verifyCpr` when `created == false`.

```
normalize(s):
  1. java.text.Normalizer.normalize(s, Normalizer.Form.NFD)
  2. remove all characters matching Unicode block "Combining Diacritical Marks"
     (regex: \p{InCombiningDiacriticalMarks})
  3. toLowerCase(Locale.ROOT)
  4. strip()

namesMatch(stored, firstName, lastName):
  candidate = normalize(firstName + " " + lastName)
  return normalize(stored).equals(candidate)
```

The candidate string is always `firstName + " " + lastName` (single space, no trimming of individual parts before concatenation — inputs arrive already clean from the caller).

---

## 4. Creditor Portal — `PersonRegistryClient`

**File:** `opendebt-creditor-portal/src/main/java/dk/ufst/opendebt/creditor/client/PersonRegistryClient.java`

### 4.1 Inner DTOs (package-private static records, defined at bottom of class)

```java
// Request to POST /persons/lookup
private record PersonLookupRequest(
    String identifier,
    String identifierType,
    String role) {}

// Response from POST /persons/lookup
private record PersonLookupResponse(
    java.util.UUID personId,
    boolean created,
    String role) {}

// Response from GET /persons/{personId}  (only fields we use)
private record PersonFetchResponse(
    java.util.UUID id,
    String name) {}

// Request to POST /organizations/lookup
private record OrganizationLookupRequest(String cvr) {}

// Response from POST /organizations/lookup
private record OrganizationLookupResponse(java.util.UUID organizationId) {}
```

Jackson deserializes these by field name; no `@JsonProperty` needed provided Jackson's object mapper (the one auto-configured by Spring Boot WebFlux) can match camelCase JSON keys.

### 4.2 `verifyCpr(String cprNumber, String firstName, String lastName)`

**Precondition:** called only with a non-null, non-blank CPR number.  
**Annotations kept as-is:** `@CircuitBreaker(name = "person-registry", fallbackMethod = "verifyCprFallback")` and `@Retry(name = "person-registry")`.

**Remove:** the inner `try/catch` block and the `log.debug` stub line.

**Implementation logic:**

```
step 1 — POST /person-registry/api/v1/persons/lookup
  request body: PersonLookupRequest(identifier=cprNumber, identifierType="CPR", role="PERSONAL")
  log.debug before call: "Verifying CPR debtor: {}", maskIdentifier(cprNumber)
    [maskIdentifier is unchanged: "****" + last-4-chars]
  on 4xx: WebClient raises WebClientResponseException → propagates out of method
          (the existing fallback rethrows 4xx; this is already handled)
  on 5xx: WebClient raises WebClientResponseException → circuit breaker fires → fallback

step 2 — branch on response.created
  IF created == true:
    return DebtorVerificationResultDto.builder()
        .verified(true)
        .personId(response.personId())
        .displayName(firstName + " " + lastName)
        .build()

  IF created == false:
    step 2a — GET /person-registry/api/v1/persons/{response.personId()}
      log.debug before call: "Fetching stored name for person: {}", response.personId()
      on 404 or 410: return DebtorVerificationResultDto.builder()
                         .verified(false)
                         .errorMessage("Navn matcher ikke")
                         .build()
        [404/410 must NOT trigger the circuit breaker — resolve to a safe result
         inside the method body before returning; see §4.2.1]
      on other 4xx: propagate WebClientResponseException (same as step 1)
      on 5xx: propagate to circuit breaker → fallback

    step 2b — null-name guard and name comparison
      IF fetchResponse.name() IS null:   [PersonDto.name is optional — guard against NPE in normalize()]
        return DebtorVerificationResultDto.builder()
            .verified(false)
            .errorMessage("Navn matcher ikke")
            .build()
      IF namesMatch(fetchResponse.name(), firstName, lastName):   [see §3]
        return DebtorVerificationResultDto.builder()
            .verified(true)
            .personId(response.personId())
            .displayName(fetchResponse.name())
            .build()
      ELSE:
        return DebtorVerificationResultDto.builder()
            .verified(false)
            .errorMessage("Navn matcher ikke")
            .build()
```

#### 4.2.1 Handling 404/410 inside `verifyCpr` without triggering circuit breaker

Use `onErrorResume` to catch 404/410 and return an empty `Mono`, then treat a `null` block result as "name mismatch". The `Mono.<Throwable>empty()` type witness is required: `onStatus` expects `Function<ClientResponse, Mono<? extends Throwable>>` and the compiler cannot infer the type parameter from untyped `Mono.empty()`.

```java
PersonFetchResponse fetched = webClient.get()
    .uri("/person-registry/api/v1/persons/{id}", lookupResp.personId())
    .retrieve()
    .onStatus(
        status -> status.value() == 404 || status.value() == 410,
        response -> response.releaseBody().then(Mono.<Throwable>empty()))
    .bodyToMono(PersonFetchResponse.class)
    .block();

if (fetched == null || fetched.name() == null || !namesMatch(fetched.name(), firstName, lastName)) {
    return DebtorVerificationResultDto.builder()
        .verified(false)
        .errorMessage("Navn matcher ikke")
        .build();
}
```

`Mono.<Throwable>empty()` resolves to `null` after `.block()`. No exception is raised, so the circuit breaker is not triggered. The compound null guard (`fetched == null || fetched.name() == null`) prevents a `NullPointerException` inside `normalize()` when the person-registry returns a person record with no name set — `name` is optional in `PersonLookupRequest` and may be absent in the stored record. This is deliberate: a null name is treated as a name mismatch ("Navn matcher ikke"), not as an error.

**Fallback method `verifyCprFallback` — no changes to signature or body.**

### 4.3 `verifyCvr(String cvrNumber)`

**Annotations kept as-is:** `@CircuitBreaker(name = "person-registry", fallbackMethod = "verifyCvrFallback")` and `@Retry(name = "person-registry")`.

**Remove:** the inner `try/catch` block and the stub log line.

**Implementation logic:**

```
log.debug: "Verifying CVR debtor: {}", cvrNumber

POST /person-registry/api/v1/organizations/lookup
  request body: OrganizationLookupRequest(cvr=cvrNumber)
  on 4xx: propagate WebClientResponseException
  on 5xx: propagate to circuit breaker → fallback

return DebtorVerificationResultDto.builder()
    .verified(true)
    .personId(response.organizationId())
    .displayName("CVR: " + cvrNumber)
    .build()
```

**Fallback method `verifyCvrFallback` — no changes to signature or body.**

### 4.4 `verifySe(String seNumber)`

**Annotations kept as-is:** `@CircuitBreaker(name = "person-registry", fallbackMethod = "verifySeeFallback")` and `@Retry(name = "person-registry")`.  
Note: the fallback method name is `verifySeeFallback` (double 'e') — **do not rename it**.

**Remove:** the inner `try/catch` block and the stub log line.

**Implementation logic:**

```
log.debug: "Verifying SE debtor: {}", seNumber

POST /person-registry/api/v1/organizations/lookup
  request body: OrganizationLookupRequest(cvr=seNumber)
    [SE number is passed as the "cvr" field — this is intentional]
  on 4xx: propagate WebClientResponseException
  on 5xx: propagate to circuit breaker → fallback

return DebtorVerificationResultDto.builder()
    .verified(true)
    .personId(response.organizationId())
    .displayName("SE: " + seNumber)
    .build()
```

**Fallback method `verifySeeFallback` — no changes to signature or body.**

### 4.5 Webclient call pattern for creditor portal

Use the same pattern as `DebtServiceClient` in the creditor portal:

```java
webClient
    .post()
    .uri("/person-registry/api/v1/persons/lookup")
    .bodyValue(new PersonLookupRequest(cprNumber, "CPR", "PERSONAL"))
    .retrieve()
    .onStatus(HttpStatusCode::is4xxClientError,
        response -> response.bodyToMono(String.class)
            .flatMap(body -> Mono.error(new WebClientResponseException(...))))
    .onStatus(HttpStatusCode::is5xxServerError,
        response -> Mono.error(new WebClientResponseException(...)))
    .bodyToMono(PersonLookupResponse.class)
    .block()
```

For 4xx handling, the fallback methods already rethrow `WebClientResponseException` when `is4xxClientError()` — so any `WebClientResponseException` with a 4xx status that escapes the method body will be rethrown by the fallback, propagating to the controller. This is the required behavior: 4xx is a client error (bad input), not a circuit breaker event.

---

## 5. Caseworker Portal — `PersonRegistryClient`

**File:** `opendebt-caseworker-portal/src/main/java/dk/ufst/opendebt/caseworker/client/PersonRegistryClient.java`

### 5.1 Inner DTOs (package-private static records, defined at bottom of class)

```java
// Response from GET /persons/{personId}  (only fields we use)
private record PersonFetchResponse(
    java.util.UUID id,
    String name) {}
```

### 5.2 `getDisplayName(UUID personId)`

**Annotations kept as-is:** `@CircuitBreaker(name = "person-registry", fallbackMethod = "getDisplayNameFallback")`.  
No `@Retry` is present; do not add one.

**Remove:** the inner `try/catch` block and the stub lines that manufacture "Person-" + shortId.

**Implementation logic:**

```
IF personId == null:
  return "—"          [unchanged — already correct in current stub]

log.debug: "Fetching display name for person: {}", personId
  [personId (UUID) is not PII; logging it is permitted]

GET /person-registry/api/v1/persons/{personId}
  on 404:  return "—"   [do NOT throw; do NOT open circuit breaker]
  on 410:  return "—"   [same treatment as 404]
  on other 4xx: let WebClientResponseException propagate
                [getDisplayNameFallback returns "—" for all Throwable,
                 so this will resolve to "—" through the fallback]
  on 5xx:  let WebClientResponseException propagate → circuit breaker → fallback → "—"

IF response.name() == null OR response.name().isBlank():
  return "—"

return response.name()
```

**404/410 handling (same technique as §4.2.1):**

```java
PersonFetchResponse fetched = webClient.get()
    .uri("/person-registry/api/v1/persons/{id}", personId)
    .retrieve()
    .onStatus(
        status -> status.value() == 404 || status.value() == 410,
        response -> response.releaseBody().then(Mono.<Throwable>empty()))
    .bodyToMono(PersonFetchResponse.class)
    .block();   // null when 404/410

if (fetched == null || fetched.name() == null || fetched.name().isBlank()) {
    return "—";
}
return fetched.name();
```

**Fallback method `getDisplayNameFallback` — no changes to signature or body.**

---

## 6. Required Imports

### Creditor portal additions

```java
import java.text.Normalizer;
import org.springframework.http.HttpStatusCode;
import reactor.core.publisher.Mono;
```

`WebClient`, `@Value`, `@Component`, `@Slf4j`, `@CircuitBreaker`, `@Retry`, `UUID`, `DebtorVerificationResultDto` are already imported.

### Caseworker portal additions

```java
import org.springframework.http.HttpStatusCode;
import reactor.core.publisher.Mono;
```

`WebClient`, `@Value`, `@Component`, `@Slf4j`, `@CircuitBreaker`, `UUID` are already imported.

---

## 7. What Must NOT Change

| Item | Reason |
|---|---|
| Class name, package, annotations (`@Slf4j`, `@Component`) | Existing Spring wiring |
| Constructor signature and WebClient construction | Already correct |
| `@CircuitBreaker` and `@Retry` annotation names and parameters | Pre-configured in `application.yml` |
| All fallback method signatures and bodies | Circuit breaker contract |
| `ERR_PERSON_REGISTRY_UNAVAILABLE` constant name and value | Used by fallback returns |
| `maskIdentifier(String)` private method | Unchanged |
| `null` check for `personId` at top of `getDisplayName` | Correct guard, already present |

---

## 8. Acceptance Criteria

### AC-1 — `verifyCpr`: new person (created=true)

```gherkin
Given person-registry returns 200 from POST /persons/lookup with created=true and personId=<uuid>
When verifyCpr("0101800001", "Jens", "Jensen") is called
Then result.verified == true
And result.personId == <uuid>
And result.displayName == "Jens Jensen"
And result.errorMessage == null
And POST /person-registry/api/v1/persons/lookup was called exactly once
And no GET /person-registry/api/v1/persons/* was called
```

### AC-2 — `verifyCpr`: existing person, names match

```gherkin
Given POST /persons/lookup returns created=false, personId=<uuid>
And GET /persons/<uuid> returns name="Jens Jensen"
When verifyCpr("0101800001", "Jens", "Jensen") is called
Then result.verified == true
And result.displayName == "Jens Jensen"
And result.personId == <uuid>
```

### AC-3 — `verifyCpr`: existing person, names match with accent normalization

```gherkin
Given POST /persons/lookup returns created=false, personId=<uuid>
And GET /persons/<uuid> returns name="Søren Ångström"
When verifyCpr("0101800001", "Søren", "Ångström") is called
Then result.verified == true
And result.displayName == "Søren Ångström"
```

### AC-4 — `verifyCpr`: existing person, names do not match

```gherkin
Given POST /persons/lookup returns created=false, personId=<uuid>
And GET /persons/<uuid> returns name="Søren Jensen"
When verifyCpr("0101800001", "Jens", "Jensen") is called
Then result.verified == false
And result.errorMessage == "Navn matcher ikke"
And result.personId == null
```

### AC-5 — `verifyCpr`: existing person, GET returns 404

```gherkin
Given POST /persons/lookup returns created=false, personId=<uuid>
And GET /persons/<uuid> returns HTTP 404
When verifyCpr("0101800001", "Jens", "Jensen") is called
Then result.verified == false
And result.errorMessage == "Navn matcher ikke"
And no exception is thrown
```

### AC-5a — `verifyCpr`: existing person, name is null in response

```gherkin
Given POST /persons/lookup returns created=false, personId=<uuid>
And GET /persons/<uuid> returns HTTP 200 with body {"id": "<uuid>", "name": null}
When verifyCpr("0101800001", "Jens", "Jensen") is called
Then result.verified == false
And result.errorMessage == "Navn matcher ikke"
And no NullPointerException is thrown
```

> **Rationale:** `PersonLookupRequest.name` is optional; a person may be stored without a name. `normalize(null)` throws NPE. The null guard in §4.2.1 intercepts this before calling `namesMatch`, treating a null name as a non-match.

### AC-6 — `verifyCpr`: person-registry POST returns 5xx → circuit breaker fallback

```gherkin
Given POST /persons/lookup returns HTTP 503
When verifyCpr("0101800001", "Jens", "Jensen") is called
Then result.verified == false
And result.errorMessage == "Person registry is unavailable. Please try again later."
```

### AC-7 — `verifyCpr`: person-registry POST returns 4xx → exception propagated

```gherkin
Given POST /persons/lookup returns HTTP 400
When verifyCpr("0101800001", "Jens", "Jensen") is called
Then WebClientResponseException is thrown (not swallowed)
```

### AC-8 — `verifyCpr`: CPR number is never logged in plain form

```gherkin
Given any call to verifyCpr with cprNumber="0101800001"
Then no log output contains "0101800001"
And log output contains "****0001"
```

### AC-9 — `verifyCvr`: success

```gherkin
Given POST /organizations/lookup returns organizationId=<uuid>
When verifyCvr("12345678") is called
Then result.verified == true
And result.personId == <uuid>
And result.displayName == "CVR: 12345678"
And POST /person-registry/api/v1/organizations/lookup was called with body {"cvr":"12345678"}
```

### AC-10 — `verifyCvr`: 5xx → fallback

```gherkin
Given POST /organizations/lookup returns HTTP 500
When verifyCvr("12345678") is called
Then result.verified == false
And result.errorMessage == "Person registry is unavailable. Please try again later."
```

### AC-11 — `verifySe`: success

```gherkin
Given POST /organizations/lookup returns organizationId=<uuid>
When verifySe("87654321") is called
Then result.verified == true
And result.personId == <uuid>
And result.displayName == "SE: 87654321"
And POST /person-registry/api/v1/organizations/lookup was called with body {"cvr":"87654321"}
```

### AC-12 — `verifySe`: 5xx → fallback

```gherkin
Given POST /organizations/lookup returns HTTP 500
When verifySe("87654321") is called
Then result.verified == false
And result.errorMessage == "Person registry is unavailable. Please try again later."
```

### AC-13 — `getDisplayName`: null personId

```gherkin
When getDisplayName(null) is called
Then result == "—"
And no HTTP call is made
```

### AC-14 — `getDisplayName`: success with name

```gherkin
Given GET /persons/<uuid> returns name="Jens Jensen"
When getDisplayName(<uuid>) is called
Then result == "Jens Jensen"
```

### AC-15 — `getDisplayName`: name is null in response

```gherkin
Given GET /persons/<uuid> returns name=null
When getDisplayName(<uuid>) is called
Then result == "—"
```

### AC-16 — `getDisplayName`: name is blank in response

```gherkin
Given GET /persons/<uuid> returns name="   "
When getDisplayName(<uuid>) is called
Then result == "—"
```

### AC-17 — `getDisplayName`: person not found (404)

```gherkin
Given GET /persons/<uuid> returns HTTP 404
When getDisplayName(<uuid>) is called
Then result == "—"
And no exception is thrown
```

### AC-18 — `getDisplayName`: 5xx → circuit breaker fallback

```gherkin
Given GET /persons/<uuid> returns HTTP 503
When getDisplayName(<uuid>) is called
Then result == "—"
```

---

## 9. Test Structure

### 9.0 `PersonNotFoundException` HTTP status unit test (`opendebt-person-registry`)

**File to create:**
```
opendebt-person-registry/src/test/java/dk/ufst/opendebt/personregistry/controller/PersonControllerNotFoundTest.java
```

**Pattern:** `@WebMvcTest` of `PersonController` with a mocked `PersonService` that throws `PersonNotFoundException`. No `@ControllerAdvice` wiring needed — `@ResponseStatus` on the exception class is picked up automatically.

```java
package dk.ufst.opendebt.personregistry.controller;

// @WebMvcTest(PersonController.class)
// Mock PersonService bean
// one @Test per AC-EH-1 and AC-EH-2
// for AC-EH-1: mock PersonService.getPersonById to throw PersonNotFoundException; GET /persons/{unknown-id}
//   assert: status 404
// for AC-EH-2: mock PersonService.getPersonById to throw PersonNotFoundException (soft-delete path)
//   assert: status 404
// assert: response body does NOT contain a "trace" or "message" field
```

**This test covers AC-EH-1 and AC-EH-2 from §0.3.**

### 9.1 Creditor portal test file

**File to create:**  
`opendebt-creditor-portal/src/test/java/dk/ufst/opendebt/creditor/client/PersonRegistryClientTest.java`

**Pattern:** identical to `DebtServiceClientTest` and `CaseServiceClientTest` in the same package.

```java
package dk.ufst.opendebt.creditor.client;

class PersonRegistryClientTest {

  private MockWebServer mockWebServer;
  private PersonRegistryClient client;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();
    baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    client = new PersonRegistryClient(WebClient.builder(), baseUrl);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  // one @Test method per acceptance criterion AC-1 through AC-12, plus AC-5a
  // use mockWebServer.enqueue(new MockResponse()...) for setup
  // use mockWebServer.takeRequest() to assert request body/path/method
  // assert results with assertThat(...)
  // for AC-7: assertThatThrownBy(...).isInstanceOf(WebClientResponseException.class)
  // for AC-8: verify maskIdentifier output via log capture or by asserting request path
  //           does not include raw CPR — log capture is optional; the spec enforces it
  //           through the maskIdentifier method which is not changed
}
```

**Multi-step scenarios** (AC-2 through AC-5, AC-5a) require two enqueued responses:

```java
mockWebServer.enqueue(/* POST /persons/lookup response */);
mockWebServer.enqueue(/* GET /persons/{id} response */);
```

Assert both requests using two `mockWebServer.takeRequest()` calls in order.

### 9.2 Caseworker portal test file

**File to create:**  
`opendebt-caseworker-portal/src/test/java/dk/ufst/opendebt/caseworker/client/PersonRegistryClientTest.java`

**No existing test class exists in the caseworker portal client package** — create from scratch using the same MockWebServer pattern:

```java
package dk.ufst.opendebt.caseworker.client;

class PersonRegistryClientTest {

  private MockWebServer mockWebServer;
  private PersonRegistryClient client;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();
    baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    client = new PersonRegistryClient(WebClient.builder(), baseUrl);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  // one @Test method per acceptance criterion AC-13 through AC-18
}
```

### 9.3 What the tests must NOT do

- Must not use `@SpringBootTest` or load any Spring context.
- Must not mock `WebClient` with Mockito — use `MockWebServer` exclusively.
- Must not test fallback method behavior directly (fallback methods require the circuit breaker to be open; testing via MockWebServer 5xx is sufficient for AC-6, AC-10, AC-12, AC-18 without a full resilience4j test).

> **Note on AC-6/AC-10/AC-12/AC-18:** When `MockWebServer` returns a 5xx and there is no circuit breaker active (unit test context), the `WebClientResponseException` will propagate rather than going through the fallback. These tests should assert `assertThatThrownBy(...).isInstanceOf(WebClientResponseException.class)` and note in a comment that the fallback behavior is verified by the existing fallback method body returning the correct `errorMessage`. Alternatively, if the test setup can invoke the fallback method directly, assert its return value. **Do not add a `@SpringBootTest` just to test the fallback.**

---

## 10. Validation Checklist

- [x] Every AC traces to a business rule stated in TB-021
- [x] Every implementation step is unambiguous (exact HTTP method, path, body fields, return construction)
- [x] Name comparison algorithm is fully specified (NFD, diacritics strip, locale-root lowercase)
- [x] 404/410 handling is specified without triggering the circuit breaker
- [x] GDPR constraint (CPR never logged) is enforced by the unchanged `maskIdentifier` method
- [x] `@ResponseStatus(HttpStatus.NOT_FOUND)` specified for `PersonNotFoundException` in `opendebt-person-registry`; maps `PersonNotFoundException` → HTTP 404 (makes AC-5 and AC-17 testable)
- [x] Null-name guard in `verifyCpr` prevents NPE in `normalize()` when `PersonFetchResponse.name()` is null (AC-5a)
- [x] `Mono.<Throwable>empty()` type witness used in all `onStatus` 404/410 handlers (§4.2.1, §5.2)
- [x] No new classes outside the two client files (`opendebt-person-registry` change is a one-line annotation addition)
- [x] Fallback signatures unchanged
- [x] `ERR_PERSON_REGISTRY_UNAVAILABLE` constant value unchanged
- [x] Test file locations and setup pattern specified (`PersonControllerNotFoundTest` using `@WebMvcTest` of `PersonController`)
- [x] No Spring context required in client unit tests
