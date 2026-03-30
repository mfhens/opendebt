// [STUB — expand with real relationships and deployment views]
// Canonical C4 architecture model for OpenDebt.
// Maintained by: solution-architect, implementation-doc-sync
// Validated by: c4-model-validator, c4-architecture-governor
// Policies enforced: architecture/policies.yaml

workspace "OpenDebt" "Architecture model for OpenDebt — open-source debt collection system for Danish public institutions (UFST Horizontale Driftsplatform). Replaces legacy EFI/DMI with PSRM-compatible microservices." {

    model {

        // ---------------------------------------------------------------
        // Actors
        // ---------------------------------------------------------------
        debtor    = person "Debtor (Skyldner)"            "Citizen or entity that owes a debt managed by OpenDebt"                              "User"
        creditor  = person "Creditor (Fordringshaver)"    "Public institution or authority that has submitted a debt claim"                     "User"
        caseworker = person "Caseworker (Sagsbehandler)"  "UFST employee managing and processing debt collection cases"                         "User"

        // ---------------------------------------------------------------
        // External Systems (outside OpenDebt boundary)
        // ---------------------------------------------------------------

        // --- Internal infrastructure ---
        keycloak = softwareSystem "Keycloak" "Identity provider — OAuth2/OIDC authentication and authorisation for all portals and services. See ADR 0005." "internal" {
            tags "internal"
        }

        flowable = softwareSystem "Flowable" "Embedded workflow/BPM engine used for case process orchestration. See ADR 0016." "internal" {
            tags "internal"
        }

        immudb = softwareSystem "immudb" "Tamper-proof, append-only financial audit ledger. Cryptographically verifiable event log. See ADR 0029." "internal" {
            tags "internal"
        }

        // --- External / third-party ---
        dupla = softwareSystem "DUPLA" "Danish court integration system for enforcement orders (fogedretten). See ADR 0009." "external" {
            tags "external"
        }

        psrm = softwareSystem "PSRM" "Danish tax authority (SKAT) debt management system. OpenDebt is designed to be PSRM-compatible." "external" {
            tags "external"
        }

        nemIdMitId = softwareSystem "NemID/MitID" "Danish national citizen authentication broker used for debtor identity verification in the Citizen Portal." "external" {
            tags "external"
        }

        edifact = softwareSystem "EDIFACT/CREMUL/DEBMUL" "EDI standard messages (CREMUL/DEBMUL) used by creditors to submit debt claims in batch format. See ADR 0017." "external" {
            tags "external"
        }

        // GOV-009: Nemkonto — Danish public disbursement clearing system (NETS). Routed via
        // Integration Gateway per ARCH-003 (trust-boundary-enforcement). GIL § 16 stk. 1.
        nemkonto = softwareSystem "Nemkonto" "Danish public disbursement clearing system operated by NETS. Intercepts public disbursements (e.g. overskydende skat, offentlige ydelser) eligible for offsetting against outstanding debts. Governed by GIL § 16 stk. 1." "external" {
            tags "external"
        }

        // ---------------------------------------------------------------
        // OpenDebt Software System
        // ---------------------------------------------------------------
        openDebt = softwareSystem "OpenDebt" "Open-source debt collection platform for Danish public institutions. Manages case lifecycle, debt claims, payments, enforcement, and creditor/debtor communication." {

            !docs docs
            !adrs docs/adr

            // --- Portals (user-facing web applications) ---
            caseworkerPortal = container "Caseworker Portal" "Web UI for UFST caseworkers (sagsbehandlere). Case overview, manual interventions, document management." "Java 21 / Spring Boot 3.3, Thymeleaf" "Web Application"

            citizenPortal = container "Citizen Portal" "Web UI for debtors (skyldnere). View debt status, payment history, and correspondence. Authenticated via NemID/MitID." "Java 21 / Spring Boot 3.3, Thymeleaf" "Web Application"

            creditorPortal = container "Creditor Portal" "Web UI for creditors (fordringshavere). Submit and monitor debt claims, view settlement status." "Java 21 / Spring Boot 3.3, Thymeleaf" "Web Application"

            // --- Backend Services ---
            caseService = container "Case Service" "Central orchestration service. Manages the full case lifecycle from claim intake through settlement or enforcement. Coordinates all domain services." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service"

            creditorService = container "Creditor Service" "Creditor and master data management. Maintains creditor profiles, agreements, and claim submission rules." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service"

            debtService = container "Debt Service" "Fordring (debt claim) management. Handles claim registration, prioritisation, interest calculation, and offsetting (modregning)." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service" {

                // ── P058: Modregning og Korrektionspulje — component declarations ────────
                publicDisbursementEventConsumer = component "PublicDisbursementEventConsumer" "Receives PublicDisbursementEvent from Nemkonto. Validates fields and idempotency. Delegates to ModregningService.initiateModregning(). Dead-letters validation failures." "Java 21, Spring @Component, Event Consumer" "Component"

                offsettingReversalEventConsumer = component "OffsettingReversalEventConsumer" "Receives OffsettingReversalEvent from P053. Guards against duplicate KorrektionspuljeEntry. Delegates to KorrektionspuljeService.processReversal()." "Java 21, Spring @Component, Event Consumer" "Component"

                modregningService = component "ModregningService" "Orchestrates the complete three-tier modregning workflow (FR-1). Implements OffsettingService (replaces P007 stub). Handles idempotency, ledger posting via LedgerServiceClient, Digital Post outbox write, and tier-2 waiver re-run (FR-2)." "Java 21, Spring @Service, @Transactional" "Component"

                modregningsRaekkefoeigenEngine = component "ModregningsRaekkefoeigenEngine" "Executes the GIL § 7, stk. 1 three-tier allocation algorithm. Delegates tier-2 partial allocation to DaekningsRaekkefoeigenServiceClient (P057) at most once per invocation. Queries fordringer via FordringQueryPort." "Java 21, Spring @Service" "Component"

                korrektionspuljeService = component "KorrektionspuljeService" "Processes OffsettingReversalEvent: Step 1 same-fordring residual, Step 2 gendaenkning via P057, Step 3 KorrektionspuljeEntry creation. Settles pool entries by re-invoking ModregningService." "Java 21, Spring @Service, @Transactional" "Component"

                renteGodtgoerelseService = component "RenteGodtgoerelseService" "Computes rentegodtgoerelse start date (5-banking-day exception, kildeskattelov § 62/62A exception) and effective rate from rentegodt_rate_entry table (GIL § 8b)." "Java 21, Spring @Service" "Component"

                korrektionspuljeSettlementJob = component "KorrektionspuljeSettlementJob" "Scheduled monthly (3 AM, 1st of month) and annual (4 AM, 2 Jan) jobs. Queries unsettled PSRM-target pool entries and invokes KorrektionspuljeService.settleEntry() per entry. Each settlement is its own transaction." "Java 21, Spring @Scheduled" "Component"

                daekningsRaekkefoeigenServiceClient = component "DaekningsRaekkefoeigenServiceClient" "HTTP client for P057 DaekningsRaekkefoeigenService in opendebt-payment-service. Invoked at most once per tier ordering run for tier-2 partial allocation and gendaenkning." "Java 21, Spring RestClient, HTTP/REST" "Component"

                modregningController = component "ModregningController" "REST controller. Exposes POST tier2-waiver (scope modregning:waiver, FR-2) and GET modregning-events read model (scope modregning:read, FR-5). Enforces OAuth2 scopes via @PreAuthorize." "Java 21, Spring @RestController" "Component"

                fordringQueryPort = component "FordringQueryPort" "Internal adapter for TB-040 active-fordringer queries within opendebt-debt-service (same-service, no inter-service HTTP). Exposes typed Java API getActiveFordringer(debtorPersonId, tier, payingAuthorityOrgId) backed by JPA repository. Consumed exclusively by ModregningsRaekkefoeigenEngine." "Java 21, Spring @Component, JPA" "Component"

                ledgerServiceClient = component "LedgerServiceClient" "HTTP client for payment-service bookkeeping API (ADR-0018). Posts double-entry debit/credit ledger entries for each tier allocation. Each entry references ModregningEvent.id and the GIL § 7 tier applied. Called from ModregningService within the @Transactional boundary; failure propagates and triggers rollback." "Java 21, Spring RestClient, HTTP/REST" "Component"

            }

            letterService = container "Letter Service" "Document generation and delivery. Produces legally required notices, decisions, and correspondence." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service"

            paymentService = container "Payment Service" "Payment processing, reconciliation, and GIL § 4 payment application order (dækningsrækkefølge). Tracks incoming payments, applies them sequentially by PrioritetKategori and FIFO sort key, and posts to financial audit ledger. Owns DaekningRecord persistence and immudb audit appends (P057)." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service"

            wageGarnishmentService = container "Wage Garnishment Service" "Lønindeholdelse processing. Calculates and issues wage garnishment orders to employers and the courts." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service"

            rulesEngine = container "Rules Engine" "Business rule evaluation. Evaluates case eligibility, enforcement thresholds, interest rules, and priority logic using Drools." "Java 21 / Spring Boot 3.3, Drools" "Service"

            integrationGateway = container "Integration Gateway" "SOAP/EDIFACT legacy protocol adapter. Translates between OpenDebt's internal REST APIs and external systems using legacy protocols (SOAP, EDIFACT). Built on Apache Camel." "Java 21 / Spring Boot 3.3, Apache Camel" "Service"

            personRegistry = container "Person Registry" "GDPR-isolated PII store. The ONLY container that persists personal data (CPR, CVR, names, addresses, contact details). CPR numbers are encrypted at rest. All other services reference persons by technical UUID only. See ADR 0014." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service" {
                tags "pii:true"
            }

            // Note: commonLib is a shared Java library (JAR), not a deployable container.
            // It provides shared utilities (logging, error handling, domain events) consumed
            // by all services at compile time. It is NOT represented as a container in the C4 model.

        }

        // ---------------------------------------------------------------
        // Relationships — Actors to Portals
        // ---------------------------------------------------------------
        caseworker -> caseworkerPortal "Uses" "HTTPS"
        debtor     -> citizenPortal    "Uses" "HTTPS"
        creditor   -> creditorPortal   "Uses" "HTTPS"

        // ---------------------------------------------------------------
        // Relationships — Portals to Backend Services
        // ---------------------------------------------------------------
        caseworkerPortal -> caseService     "Manages cases via"           "HTTPS/REST"
        caseworkerPortal -> letterService   "Requests documents via"      "HTTPS/REST"
        citizenPortal    -> caseService     "Reads case status via"       "HTTPS/REST"
        citizenPortal    -> paymentService  "Views payment history via"   "HTTPS/REST"
        creditorPortal   -> creditorService "Manages creditor data via"   "HTTPS/REST"
        creditorPortal   -> debtService     "Submits claims via"          "HTTPS/REST"

        // M2M / API path — Petition 011: fordringshaver system submits fordringer via OCES3 certificate
        creditor         -> integrationGateway "Submits fordringer via API with OCES3 certificate (M2M)" "HTTPS/REST"
        integrationGateway -> debtService    "Routes M2M fordring submissions to"  "HTTPS/REST"

        // ---------------------------------------------------------------
        // Relationships — Service to Service (core orchestration)
        // ---------------------------------------------------------------
        caseService -> debtService             "Retrieves and updates debt claims"          "HTTPS/REST"
        caseService -> creditorService         "Reads creditor master data"                 "HTTPS/REST"
        caseService -> paymentService          "Triggers payment allocation"                "HTTPS/REST"
        caseService -> letterService           "Requests generation of notices"             "HTTPS/REST"
        caseService -> wageGarnishmentService  "Initiates garnishment proceedings"          "HTTPS/REST"
        caseService -> rulesEngine             "Evaluates business rules"                   "HTTPS/REST"
        caseService -> personRegistry          "Resolves person data by UUID"               "HTTPS/REST"

        debtService            -> personRegistry  "Resolves person data by UUID"           "HTTPS/REST"

        // ── P058: Modregning og Korrektionspulje — container-level relationships ──────────
        // GOV-001: P057 delegation (DaekningsRaekkefoeigenServiceClient → payment-service)
        debtService -> paymentService "Delegates tier-2 partial allocation to DaekningsRaekkefoeigenService (P057, ADR-0007)" "HTTPS/REST"
        // GOV-002: ADR-0018 ledger posting (LedgerServiceClient → payment-service)
        debtService -> paymentService "Posts double-entry ledger entries via LedgerServiceClient (ADR-0018)" "HTTPS/REST"
        // GOV-003: ADR-0029 tamper-proof audit log (post-commit gRPC append)
        debtService -> immudb "Appends ModregningEvent and SET_OFF CollectionMeasure records (post-commit, ADR-0029)" "gRPC"
        // GOV-004: W-004 fix — use Async/Outbox label; PostgreSQL/JPA would violate ARCH-001/ARCH-009
        debtService -> letterService "Sends notification outbox events to (Transactional Outbox, ADR-0019)" "Async/Outbox"
        // GOV-007 / ARCH-011: All HTTP endpoints must validate tokens via Keycloak
        debtService -> keycloak "Validates tokens via" "OAuth2/OIDC"
        // GOV-005: caseworkerPortal now calls modregning endpoints (FR-2, FR-5)
        caseworkerPortal -> debtService "Submits modregning tier-2 waivers and reads modregning events via" "HTTPS/REST"

        paymentService         -> personRegistry  "Resolves person data by UUID"           "HTTPS/REST"
        letterService          -> personRegistry  "Resolves person data for addressing"    "HTTPS/REST"
        wageGarnishmentService -> personRegistry  "Resolves person data by UUID"           "HTTPS/REST"
        creditorService        -> personRegistry  "Resolves person data by UUID"           "HTTPS/REST"

        paymentService -> immudb "Appends payment audit events"  "gRPC"

        // ---------------------------------------------------------------
        // Relationships — Integration Gateway to External Systems
        // ---------------------------------------------------------------
        integrationGateway -> dupla      "Submits enforcement orders"          "SOAP/HTTPS"
        integrationGateway -> psrm       "Exchanges debt data"                 "SOAP/HTTPS"
        integrationGateway -> edifact    "Receives creditor batch submissions" "EDIFACT/SFTP"
        caseService        -> integrationGateway "Routes external integrations through" "HTTPS/REST"

        // GOV-009: Nemkonto routed via Integration Gateway (ARCH-003 trust-boundary-enforcement)
        integrationGateway -> nemkonto    "Receives disbursement interception events from" "HTTPS/Event"
        integrationGateway -> debtService "Forwards PublicDisbursementEvent for modregning processing" "Async/Messaging"

        // ---------------------------------------------------------------
        // Relationships — Cross-cutting Infrastructure
        // ---------------------------------------------------------------
        caseworkerPortal -> paymentService  "Reads daekningsraekkefoelge (GIL § 4 P057) via" "HTTPS/REST"
        caseworkerPortal -> keycloak "Authenticates users via"  "OAuth2/OIDC"
        citizenPortal    -> keycloak "Authenticates users via"  "OAuth2/OIDC"
        creditorPortal   -> keycloak "Authenticates users via"  "OAuth2/OIDC"
        caseService      -> keycloak "Validates tokens via"     "OAuth2/OIDC"
        // [STUB — add token validation relationships for remaining services]

        citizenPortal -> nemIdMitId "Delegates citizen authentication to" "OIDC redirect"
        keycloak      -> nemIdMitId "Federates citizen identity from"     "OIDC"

        caseService -> flowable "Executes workflow processes via" "Embedded/API"

        // PostgreSQL is infrastructure — each service owns its own DB instance.
        // Modelled at deployment-view level (see stub below), not at system-context level.
        // See ADR 0007, ADR 0011.

        // ── P058: Modregning og Korrektionspulje — component-level relationships (SA-058 §11 Section B) ─
        // Event consumers → services
        publicDisbursementEventConsumer -> modregningService "Delegates PublicDisbursementEvent to" "Java method call"
        offsettingReversalEventConsumer -> korrektionspuljeService "Delegates OffsettingReversalEvent to" "Java method call"

        // ModregningService orchestration
        modregningService -> modregningsRaekkefoeigenEngine "Delegates three-tier allocation to" "Java method call"
        modregningService -> renteGodtgoerelseService "Computes rentegodtgoerelse start date via" "Java method call"
        modregningService -> ledgerServiceClient "Posts SET_OFF ledger entries (ADR-0018)" "HTTPS/REST"
        modregningService -> immudb "Appends ModregningEvent and SET_OFF CollectionMeasure records (post-commit, ADR-0029)" "gRPC"

        // ModregningsRaekkefoeigenEngine — fordring query (same-service port, no self-ref) and P057 delegation
        modregningsRaekkefoeigenEngine -> fordringQueryPort "Queries active fordringer by tier (TB-040)" "Java method call"
        modregningsRaekkefoeigenEngine -> daekningsRaekkefoeigenServiceClient "Delegates tier-2 partial allocation (at most once per run)" "Java method call"

        // HTTP clients → payment-service
        daekningsRaekkefoeigenServiceClient -> paymentService "Calls DaekningsRaekkefoeigenService for GIL § 4 allocation (ADR-0007)" "HTTPS/REST"
        ledgerServiceClient -> paymentService "Posts double-entry ledger entries" "HTTPS/REST"

        // KorrektionspuljeService
        korrektionspuljeService -> daekningsRaekkefoeigenServiceClient "Delegates gendaenkning Step 2 allocation to" "Java method call"
        korrektionspuljeService -> modregningService "Re-enters initiateModregning for pool settlement" "Java method call"
        korrektionspuljeService -> renteGodtgoerelseService "Computes accrued rentegodtgoerelse rate at settlement" "Java method call"

        // KorrektionspuljeSettlementJob
        korrektionspuljeSettlementJob -> korrektionspuljeService "Invokes settleEntry() per unsettled PSRM pool entry" "Java method call"

        // ModregningController
        modregningController -> modregningService "Invokes applyTier2Waiver() for FR-2 waiver" "Java method call"
        caseworkerPortal -> modregningController "Reads modregning-events and submits tier-2 waiver via" "HTTPS/REST"

    }

    views {

        systemContext openDebt "SystemContext" "System context view for OpenDebt — shows actors, the OpenDebt system, and all external integrations." {
            include *
            autoLayout lr
        }

        container openDebt "Containers" "Container view for OpenDebt — shows all deployable services and their primary relationships." {
            include *
            autoLayout lr
        }

        // [STUB — add deploymentEnvironment views when infrastructure (Kubernetes namespaces,
        //  node pools, ingress) is defined. Required by ARCH-005 before deployment gate.]
        //
        // Example skeleton:
        // deploymentEnvironment "OpenDebt" "Production" {
        //     deploymentNode "Kubernetes Cluster" "Azure AKS" "Kubernetes 1.29" {
        //         deploymentNode "opendebt namespace" "" "Kubernetes namespace" {
        //             containerInstance caseService
        //             containerInstance debtService
        //             // ... etc.
        //         }
        //     }
        // }

        // ── P058: Modregning og Korrektionspulje — component view (SA-058 §11 Section C) ──
        component debtService "DebtService_P058_Components" "P058 component view — Modregning og Korrektionspulje. Shows all new components within opendebt-debt-service and their relationships to P057 (payment-service), immudb, and caseworkerPortal." {
            include publicDisbursementEventConsumer
            include offsettingReversalEventConsumer
            include modregningService
            include modregningsRaekkefoeigenEngine
            include korrektionspuljeService
            include renteGodtgoerelseService
            include korrektionspuljeSettlementJob
            include daekningsRaekkefoeigenServiceClient
            include modregningController
            include fordringQueryPort
            include ledgerServiceClient
            include paymentService
            include immudb
            include caseworkerPortal
            autoLayout lr
        }

        theme default
    }

    configuration {
        scope "softwareSystem"
    }

}
