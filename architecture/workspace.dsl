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

        postgresql = softwareSystem "PostgreSQL" "Primary relational datastore. Each service owns its own isolated database instance — no cross-service DB access. See ADR 0007, ADR 0011." "internal" {
            tags "internal"
        }

        flowable = softwareSystem "Flowable" "Embedded workflow/BPM engine used for case process orchestration. See ADR 0016." "internal" {
            tags "internal"
        }

        immudb = softwareSystem "immudb" "Tamper-proof, append-only financial audit ledger. Cryptographically verifiable event log. See ADR 0029." "internal" {
            tags "internal"
        }

        kubernetes = softwareSystem "Kubernetes" "Container orchestration platform. All OpenDebt services are deployed as Kubernetes workloads. See ADR 0006." "internal" {
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

        // ---------------------------------------------------------------
        // OpenDebt Software System
        // ---------------------------------------------------------------
        openDebt = softwareSystem "OpenDebt" "Open-source debt collection platform for Danish public institutions. Manages case lifecycle, debt claims, payments, enforcement, and creditor/debtor communication." {

            // --- Portals (user-facing web applications) ---
            caseworkerPortal = container "Caseworker Portal" "Web UI for UFST caseworkers (sagsbehandlere). Case overview, manual interventions, document management." "Java 21 / Spring Boot 3.3, Thymeleaf" "Web Application"

            citizenPortal = container "Citizen Portal" "Web UI for debtors (skyldnere). View debt status, payment history, and correspondence. Authenticated via NemID/MitID." "Java 21 / Spring Boot 3.3, Thymeleaf" "Web Application"

            creditorPortal = container "Creditor Portal" "Web UI for creditors (fordringshavere). Submit and monitor debt claims, view settlement status." "Java 21 / Spring Boot 3.3, Thymeleaf" "Web Application"

            // --- Backend Services ---
            caseService = container "Case Service" "Central orchestration service. Manages the full case lifecycle from claim intake through settlement or enforcement. Coordinates all domain services." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service"

            creditorService = container "Creditor Service" "Creditor and master data management. Maintains creditor profiles, agreements, and claim submission rules." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service"

            debtService = container "Debt Service" "Fordring (debt claim) management. Handles claim registration, prioritisation, interest calculation, and offsetting (modregning)." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service"

            letterService = container "Letter Service" "Document generation and delivery. Produces legally required notices, decisions, and correspondence." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service"

            paymentService = container "Payment Service" "Payment processing and reconciliation. Tracks incoming payments, distributes to claims, and posts to financial audit ledger." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service"

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

        // ---------------------------------------------------------------
        // Relationships — Cross-cutting Infrastructure
        // ---------------------------------------------------------------
        caseworkerPortal -> keycloak "Authenticates users via"  "OAuth2/OIDC"
        citizenPortal    -> keycloak "Authenticates users via"  "OAuth2/OIDC"
        creditorPortal   -> keycloak "Authenticates users via"  "OAuth2/OIDC"
        caseService      -> keycloak "Validates tokens via"     "OAuth2/OIDC"
        // [STUB — add token validation relationships for remaining services]

        citizenPortal -> nemIdMitId "Delegates citizen authentication to" "OIDC redirect"
        keycloak      -> nemIdMitId "Federates citizen identity from"     "OIDC"

        caseService -> flowable "Executes workflow processes via" "Embedded/API"

        // [STUB — add PostgreSQL relationships per-service when deployment view is defined]
        // Each service connects to its own PostgreSQL database instance.
        // See ARCH-009 (no-cross-service-db-access).

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

        theme default
    }

}
