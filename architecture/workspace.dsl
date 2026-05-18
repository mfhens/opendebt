// Canonical C4 architecture model for OpenDebt.
// Maintained by: solution-architect, implementation-doc-sync
// Validated by: c4-model-validator, c4-architecture-governor
// Policies enforced: architecture/policies.yaml

workspace "OpenDebt" "Architecture model for OpenDebt — open-source debt collection system for Danish public institutions (UFST Horizontale Driftsplatform). Replaces legacy EFI/DMI with PSRM-compatible microservices." {

    model {

        // ---------------------------------------------------------------
        // Actors
        // ---------------------------------------------------------------
        debtor = person "Debtor (Skyldner)" "Citizen or entity that owes a debt managed by OpenDebt" "User"
        creditor = person "Creditor (Fordringshaver)" "Public institution or authority that has submitted a debt claim" "User"
        caseworker = person "Caseworker (Sagsbehandler)" "UFST employee managing and processing debt collection cases" "User"

        // ---------------------------------------------------------------
        // External Systems (outside OpenDebt boundary)
        // ---------------------------------------------------------------
        keycloak = softwareSystem "Keycloak" "Identity provider — OAuth2/OIDC authentication and authorisation for all portals and services. See ADR 0005." "internal" {
            tags "internal"
        }

        flowable = softwareSystem "Flowable" "Embedded workflow/BPM engine used for case process orchestration. See ADR 0016." "internal" {
            tags "internal"
        }

        immudb = softwareSystem "immudb" "Tamper-proof, append-only financial audit ledger. Cryptographically verifiable event log. See ADR 0029." "internal" {
            tags "internal"
        }

        cls = softwareSystem "CLS" "UFST Common Logging System receiving shared audit events and database-shipped audit records. See ADR 0022." "internal" {
            tags "internal"
        }

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

        nemkonto = softwareSystem "Nemkonto" "Danish public disbursement clearing system operated by NETS. Intercepts public disbursements eligible for offsetting against outstanding debts. Governed by GIL § 16 stk. 1." "external" {
            tags "external"
        }

        // ---------------------------------------------------------------
        // OpenDebt Software System
        // ---------------------------------------------------------------
        openDebt = softwareSystem "OpenDebt" "Open-source debt collection platform for Danish public institutions. Manages case lifecycle, debt claims, payments, enforcement, and creditor/debtor communication." {

            !docs docs
            !adrs adr

            caseworkerPortal = container "Caseworker Portal" "Web UI for UFST caseworkers. Presents case detail, limitation status, objection controls, and related debt actions while remaining a composition layer over backend services." "Java 21 / Spring Boot 3.3, Thymeleaf" "Web Application" {
                properties {
                    "domain.concepts" "foraeldelse, indsigelse, fordringskompleks, fordring"
                }

                limitationPanelController = component "LimitationPanelController" "BFF/controller for the petition059 limitation panel." "Spring MVC / BFF" "Component"
                limitationPanelView = component "LimitationPanelView" "Rendered view for limitation status, histories, and objection controls." "Thymeleaf view" "Component"
                limitationPortalClient = component "LimitationPortalClient" "Outbound client used by the portal to call debt-service limitation APIs." "HTTP client" "Component"
            }

            citizenPortal = container "Citizen Portal" "Web UI for debtors (skyldnere). View debt status, payment history, and correspondence. Authenticated via NemID/MitID." "Java 21 / Spring Boot 3.3, Thymeleaf" "Web Application"

            creditorPortal = container "Creditor Portal" "Web UI for creditors (fordringshavere). Submit and monitor debt claims, view settlement status." "Java 21 / Spring Boot 3.3, Thymeleaf" "Web Application"

            caseService = container "Case Service" "Central orchestration service. For petition059 it owns the limitation-objection workflow lifecycle behind an internal API consumed by debt-service." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service" {
                properties {
                    "domain.concepts" "indsigelse, foraeldelse, fordring"
                }

                limitationObjectionWorkflowInternalApi = component "LimitationObjectionWorkflowInternalApi" "Internal workflow API for registering and evaluating limitation objections." "REST API" "Component"
                limitationObjectionWorkflow = component "LimitationObjectionWorkflow" "Flowable-managed objection workflow that tracks registration, review, decision, and completion." "Flowable BPM / orchestration" "Component"
                objectionAuditPublisher = component "ObjectionAuditPublisher" "Publishes workflow audit events through the shared audit pipeline." "Audit publisher" "Component"
            }

            creditorService = container "Creditor Service" "Creditor and master data management. Maintains creditor profiles, agreements, and claim submission rules." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service"

            debtService = container "Debt Service" "Fordring management. For petition059 it additionally owns the authoritative limitation state and the external limitation surface required by the petition/outcome contract." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service" {
                properties {
                    "domain.concepts" "fordring, foraeldelse, fordringskompleks, modregning"
                }

                publicDisbursementEventConsumer = component "PublicDisbursementEventConsumer" "Receives PublicDisbursementEvent from Nemkonto. Validates fields and idempotency. Delegates to ModregningService.initiateModregning(). Dead-letters validation failures." "Java 21, Spring @Component, Event Consumer" "Component"
                offsettingReversalEventConsumer = component "OffsettingReversalEventConsumer" "Receives OffsettingReversalEvent from P053. Guards against duplicate KorrektionspuljeEntry. Delegates to KorrektionspuljeService.processReversal()." "Java 21, Spring @Component, Event Consumer" "Component"
                modregningService = component "ModregningService" "Orchestrates the complete three-tier modregning workflow. For petition059 it is also a source of legally effective set-off facts that may create limitation interruptions." "Java 21, Spring @Service, @Transactional" "Component"
                modregningsRaekkefoeigenEngine = component "ModregningsRaekkefoeigenEngine" "Executes the GIL § 7, stk. 1 three-tier allocation algorithm. Delegates tier-2 partial allocation to DaekningsRaekkefoeigenServiceClient and queries fordringer via FordringQueryPort." "Java 21, Spring @Service" "Component"
                korrektionspuljeService = component "KorrektionspuljeService" "Processes OffsettingReversalEvent and pool settlement logic for petition058." "Java 21, Spring @Service, @Transactional" "Component"
                renteGodtgoerelseService = component "RenteGodtgoerelseService" "Computes rentegodtgoerelse start date and effective rate for petition058." "Java 21, Spring @Service" "Component"
                korrektionspuljeSettlementJob = component "KorrektionspuljeSettlementJob" "Scheduled settlement job for petition058 pool entries." "Java 21, Spring @Scheduled" "Component"
                daekningsRaekkefoeigenServiceClient = component "DaekningsRaekkefoeigenServiceClient" "HTTP client for P057 daekningsraekkefoelge service in payment-service." "Java 21, Spring RestClient, HTTP/REST" "Component"
                modregningController = component "ModregningController" "REST controller for petition058 modregning actions and history reads." "Java 21, Spring @RestController" "Component"
                fordringQueryPort = component "FordringQueryPort" "Internal adapter for active-fordringer queries within debt-service." "Java 21, Spring @Component, JPA" "Component"
                ledgerServiceClient = component "LedgerServiceClient" "HTTP client for payment-service bookkeeping API used by petition058." "Java 21, Spring RestClient, HTTP/REST" "Component"

                limitationApi = component "LimitationApi" "External limitation surface, including petition-aligned POST/PUT /foraeldelse/{fordringId}/indsigelse commands." "REST API" "Component"
                limitationObjectionFacade = component "LimitationObjectionFacade" "Contract-preserving application façade for FR-6 objection commands." "Application service" "Component"
                limitationStateApplicationService = component "LimitationStateApplicationService" "Application seam for limitation reads, mutations, and interruption orchestration." "Application service" "Component"
                limitationPolicyEngine = component "LimitationPolicyEngine" "Deterministic limitation arithmetic and legal branch evaluation." "Domain service" "Component"
                claimComplexManager = component "ClaimComplexManager" "Maintains explicit claim-complex membership and applies same-transaction propagation of interruption effects across all complex members." "Domain service" "Component"
                limitationObjectionWorkflowClient = component "LimitationObjectionWorkflowClient" "Internal client from debt-service to the case-service limitation objection workflow API." "HTTP client" "Component"
                wageGarnishmentFactClient = component "WageGarnishmentFactClient" "Read-only client used by petition059 to resolve decision, notification, covered-claim, and inactivity facts." "HTTP client" "Component"
                limitationAuditPublisher = component "LimitationAuditPublisher" "Publishes limitation audit events through the shared audit pipeline." "Audit publisher" "Component"
            }

            letterService = container "Letter Service" "Document generation and delivery. Produces legally required notices, decisions, and correspondence." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service"

            paymentService = container "Payment Service" "Payment processing, reconciliation, and GIL § 4 payment application order (dækningsrækkefølge). Tracks incoming payments, applies them sequentially by PrioritetKategori and FIFO sort key, and posts to financial audit ledger. Owns DaekningRecord persistence and immudb audit appends (P057)." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service"

            wageGarnishmentService = container "Wage Garnishment Service" "Lønindeholdelse processing. Petition059 uses it as the source of decision, notification, covered-claim, and inactivity facts needed to determine whether wage garnishment interrupts limitation periods." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service" {
                properties {
                    "domain.concepts" "loenindeholdelse, underretning"
                }

                limitationFactApi = component "LimitationFactApi" "Fact-oriented internal API for petition059. Exposes the minimum wage-garnishment state required by debt-service." "REST API" "Component"
            }

            rulesEngine = container "Rules Engine" "Business rule evaluation. Evaluates case eligibility, enforcement thresholds, interest rules, and priority logic using Drools." "Java 21 / Spring Boot 3.3, Drools" "Service"

            integrationGateway = container "Integration Gateway" "SOAP/EDIFACT legacy protocol adapter. Translates between OpenDebt's internal REST APIs and external systems using legacy protocols (SOAP, EDIFACT). Built on Apache Camel." "Java 21 / Spring Boot 3.3, Apache Camel" "Service"

            personRegistry = container "Person Registry" "GDPR-isolated PII store. The ONLY container that persists personal data (CPR, CVR, names, addresses, contact details). All other services reference persons by technical UUID only. See ADR 0014." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service" {
                tags "pii:true"
            }

            // Note: shared audit infrastructure lives in opendebt-common / audit-trail-commons and is not a deployable container.
        }

        // ---------------------------------------------------------------
        // Relationships — Actors to Portals
        // ---------------------------------------------------------------
        caseworker -> caseworkerPortal "Uses" "HTTPS"
        debtor -> citizenPortal "Uses" "HTTPS"
        creditor -> creditorPortal "Uses" "HTTPS"

        // ---------------------------------------------------------------
        // Relationships — Portals to Backend Services
        // ---------------------------------------------------------------
        caseworkerPortal -> caseService "Manages cases and reads workflow metadata via" "HTTPS/REST"
        caseworkerPortal -> debtService "Reads debt detail, limitation status, modregning history, and submits debt actions via" "HTTPS/REST"
        caseworkerPortal -> letterService "Requests documents via" "HTTPS/REST"
        citizenPortal -> caseService "Reads case status via" "HTTPS/REST"
        citizenPortal -> paymentService "Views payment history via" "HTTPS/REST"
        creditorPortal -> creditorService "Manages creditor data via" "HTTPS/REST"
        creditorPortal -> debtService "Submits claims via" "HTTPS/REST"

        creditor -> integrationGateway "Submits fordringer via API with OCES3 certificate (M2M)" "HTTPS/REST"
        integrationGateway -> debtService "Routes M2M fordring submissions to" "HTTPS/REST"

        // ---------------------------------------------------------------
        // Relationships — Service to Service (core orchestration)
        // ---------------------------------------------------------------
        caseService -> debtService "Retrieves and updates debt claims" "HTTPS/REST"
        caseService -> creditorService "Reads creditor master data" "HTTPS/REST"
        caseService -> paymentService "Triggers payment allocation" "HTTPS/REST"
        caseService -> letterService "Requests generation of notices" "HTTPS/REST"
        caseService -> wageGarnishmentService "Initiates garnishment proceedings" "HTTPS/REST"
        caseService -> rulesEngine "Evaluates business rules" "HTTPS/REST"
        caseService -> personRegistry "Resolves person data by UUID" "HTTPS/REST"
        caseService -> cls "Ships workflow audit events via audit-trail-commons / ADR-0022 pipeline" "Structured audit pipeline"

        debtService -> caseService "Delegates limitation objection workflow registration/evaluation via internal API" "HTTPS/REST"
        debtService -> personRegistry "Resolves person data by UUID" "HTTPS/REST"
        debtService -> paymentService "Delegates tier-2 partial allocation to DaekningsRaekkefoeigenService (P057, ADR-0007)" "HTTPS/REST"
        debtService -> paymentService "Posts double-entry ledger entries via LedgerServiceClient (ADR-0018)" "HTTPS/REST"
        debtService -> immudb "Appends ModregningEvent and SET_OFF CollectionMeasure records (post-commit, ADR-0029)" "gRPC"
        debtService -> letterService "Sends notification outbox events to (Transactional Outbox, ADR-0019)" "Async/Outbox"
        debtService -> wageGarnishmentService "Reads wage-garnishment decision and inactivity facts for limitation interruption rules" "HTTPS/REST"
        debtService -> keycloak "Validates tokens via" "OAuth2/OIDC"
        debtService -> cls "Ships limitation audit events via audit-trail-commons / ADR-0022 pipeline" "Structured audit pipeline"

        paymentService -> personRegistry "Resolves person data by UUID" "HTTPS/REST"
        letterService -> personRegistry "Resolves person data for addressing" "HTTPS/REST"
        wageGarnishmentService -> personRegistry "Resolves person data by UUID" "HTTPS/REST"
        wageGarnishmentService -> keycloak "Validates tokens via" "OAuth2/OIDC"
        creditorService -> personRegistry "Resolves person data by UUID" "HTTPS/REST"
        paymentService -> immudb "Appends payment audit events" "gRPC"

        // ---------------------------------------------------------------
        // Relationships — Integration Gateway to External Systems
        // ---------------------------------------------------------------
        integrationGateway -> dupla "Submits enforcement orders" "SOAP/HTTPS"
        integrationGateway -> psrm "Exchanges debt data" "SOAP/HTTPS"
        integrationGateway -> edifact "Receives creditor batch submissions" "EDIFACT/SFTP"
        caseService -> integrationGateway "Routes external integrations through" "HTTPS/REST"
        integrationGateway -> nemkonto "Receives disbursement interception events from" "HTTPS/Event"
        integrationGateway -> debtService "Forwards PublicDisbursementEvent for modregning processing" "Async/Messaging"

        // ---------------------------------------------------------------
        // Relationships — Cross-cutting Infrastructure
        // ---------------------------------------------------------------
        caseworkerPortal -> paymentService "Reads daekningsraekkefoelge (GIL § 4 P057) via" "HTTPS/REST"
        caseworkerPortal -> keycloak "Authenticates users via" "OAuth2/OIDC"
        citizenPortal -> keycloak "Authenticates users via" "OAuth2/OIDC"
        creditorPortal -> keycloak "Authenticates users via" "OAuth2/OIDC"
        caseService -> keycloak "Validates tokens via" "OAuth2/OIDC"
        citizenPortal -> nemIdMitId "Delegates citizen authentication to" "OIDC redirect"
        keycloak -> nemIdMitId "Federates citizen identity from" "OIDC"
        caseService -> flowable "Executes workflow processes via" "Embedded/API"

        // ---------------------------------------------------------------
        // Component relationships — P058 + P059
        // ---------------------------------------------------------------
        publicDisbursementEventConsumer -> modregningService "Delegates PublicDisbursementEvent to" "Java method call"
        offsettingReversalEventConsumer -> korrektionspuljeService "Delegates OffsettingReversalEvent to" "Java method call"
        modregningService -> modregningsRaekkefoeigenEngine "Delegates three-tier allocation to" "Java method call"
        modregningService -> renteGodtgoerelseService "Computes rentegodtgoerelse start date via" "Java method call"
        modregningService -> ledgerServiceClient "Posts SET_OFF ledger entries (ADR-0018)" "HTTPS/REST"
        modregningService -> immudb "Appends ModregningEvent and SET_OFF CollectionMeasure records (post-commit, ADR-0029)" "gRPC"
        modregningService -> limitationStateApplicationService "Registers set-off as a legally effective limitation interruption on" "Java method call"

        modregningsRaekkefoeigenEngine -> fordringQueryPort "Queries active fordringer by tier (TB-040)" "Java method call"
        modregningsRaekkefoeigenEngine -> daekningsRaekkefoeigenServiceClient "Delegates tier-2 partial allocation (at most once per run)" "Java method call"
        daekningsRaekkefoeigenServiceClient -> paymentService "Calls DaekningsRaekkefoeigenService for GIL § 4 allocation (ADR-0007)" "HTTPS/REST"
        ledgerServiceClient -> paymentService "Posts double-entry ledger entries" "HTTPS/REST"
        korrektionspuljeService -> daekningsRaekkefoeigenServiceClient "Delegates gendaenkning Step 2 allocation to" "Java method call"
        korrektionspuljeService -> modregningService "Re-enters initiateModregning for pool settlement" "Java method call"
        korrektionspuljeService -> renteGodtgoerelseService "Computes accrued rentegodtgoerelse rate at settlement" "Java method call"
        korrektionspuljeSettlementJob -> korrektionspuljeService "Invokes settleEntry() per unsettled PSRM pool entry" "Java method call"
        modregningController -> modregningService "Invokes applyTier2Waiver() for FR-2 waiver" "Java method call"

        limitationPanelController -> limitationPortalClient "Uses for limitation reads and FR-6 actions" "Java method call"
        limitationPanelController -> limitationPanelView "Renders limitation panel state and controls" "Java method call"
        limitationPortalClient -> limitationApi "Calls debt-service limitation surface" "HTTPS/REST"
        limitationApi -> limitationObjectionFacade "Delegates petition-aligned objection commands to" "Java method call"
        limitationApi -> limitationStateApplicationService "Delegates limitation reads and direct mutations to" "Java method call"
        limitationObjectionFacade -> limitationObjectionWorkflowClient "Delegates workflow lifecycle to" "Java method call"
        limitationObjectionFacade -> limitationStateApplicationService "Applies resulting limitation-state transitions through" "Java method call"
        limitationObjectionFacade -> limitationAuditPublisher "Emits limitation audit events through" "Java method call"
        limitationStateApplicationService -> limitationPolicyEngine "Delegates date arithmetic and rule evaluation to" "Java method call"
        limitationStateApplicationService -> claimComplexManager "Uses for claim-complex membership and propagation" "Java method call"
        limitationStateApplicationService -> wageGarnishmentFactClient "Resolves wage-garnishment interruption facts through" "Java method call"
        limitationStateApplicationService -> limitationAuditPublisher "Emits state-change audit events through" "Java method call"
        claimComplexManager -> limitationPolicyEngine "Reuses deterministic limitation arithmetic from" "Java method call"
        limitationObjectionWorkflowClient -> limitationObjectionWorkflowInternalApi "Calls internal workflow contract on" "HTTPS/REST"
        limitationObjectionWorkflowInternalApi -> limitationObjectionWorkflow "Delegates workflow commands to" "Java method call"
        limitationObjectionWorkflow -> objectionAuditPublisher "Emits workflow audit events through" "Java method call"
        wageGarnishmentFactClient -> limitationFactApi "Calls to obtain decision and inactivity facts" "HTTPS/REST"
        limitationAuditPublisher -> cls "Ships limitation audit events to" "Structured audit pipeline"
        objectionAuditPublisher -> cls "Ships workflow audit events to" "Structured audit pipeline"

        // ---------------------------------------------------------------
        // Deployment environment — required by ARCH-005
        // ---------------------------------------------------------------
        deploymentEnvironment "Production" {
            deploymentNode "UFST Horizontale Driftsplatform" "Azure AKS landing zone for OpenDebt" "Azure platform" {
                deploymentNode "AKS Cluster" "OpenDebt workloads" "Kubernetes 1.29" {
                    deploymentNode "opendebt namespace" "Application namespace" "Kubernetes namespace" {
                        containerInstance caseworkerPortal
                        containerInstance citizenPortal
                        containerInstance creditorPortal
                        containerInstance caseService
                        containerInstance creditorService
                        containerInstance debtService
                        containerInstance letterService
                        containerInstance paymentService
                        containerInstance wageGarnishmentService
                        containerInstance rulesEngine
                        containerInstance integrationGateway
                        containerInstance personRegistry
                    }
                }
            }
        }
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

        deployment openDebt "Production" "ProductionDeployment" "Production deployment view for OpenDebt." {
            include *
            autoLayout lr
        }

        component debtService "DebtService_P058_Components" "P058 component view — Modregning og Korrektionspulje." {
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

        component debtService "DebtService_P059_Components" "P059 component view — limitation surface, workflow delegation, wage-garnishment facts, and CLS audit relations." {
            include limitationApi
            include limitationObjectionFacade
            include limitationStateApplicationService
            include limitationPolicyEngine
            include claimComplexManager
            include limitationObjectionWorkflowClient
            include wageGarnishmentFactClient
            include limitationAuditPublisher
            include limitationObjectionWorkflowInternalApi
            include limitationObjectionWorkflow
            include objectionAuditPublisher
            include limitationFactApi
            include caseworkerPortal
            include caseService
            include wageGarnishmentService
            include cls
            autoLayout lr
        }

        theme default
    }

    configuration {
        scope "softwareSystem"
    }
}
