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
                section50WorklistController = component "Section50WorklistController" "BFF/controller for petition060 section-50 retskraft worklist generation, inspection, and override actions." "Spring MVC / BFF" "Component"
                section50WorklistView = component "Section50WorklistView" "Rendered view for ranked section-50 worklists, amount windows, and audit explanation." "Thymeleaf view" "Component"
                section50PortalClient = component "Section50PortalClient" "Outbound client used by the portal to call debt-service section-50 APIs." "HTTP client" "Component"
            }

            citizenPortal = container "Citizen Portal" "Web UI for debtors (skyldnere). View debt status, payment history, and correspondence. Authenticated via NemID/MitID." "Java 21 / Spring Boot 3.3, Thymeleaf" "Web Application" {
                debtOverviewController = component "DebtOverviewController" "BFF/controller for GET /min-gaeld. Orchestrates the authenticated citizen debt overview page and its accessible empty/error states." "Spring MVC / BFF" "Component"
                citizenDebtClient = component "CitizenDebtClient" "Outbound client from citizen-portal to debt-service for the petition026 citizen debt summary." "HTTP client" "Component"
                debtOverviewView = component "DebtOverviewView" "Rendered Thymeleaf view for the citizen debt overview table, explanations, and configured actions." "Thymeleaf view" "Component"
            }

            creditorPortal = container "Creditor Portal" "Web UI for creditors (fordringshavere). Submit and monitor debt claims, view settlement status." "Java 21 / Spring Boot 3.3, Thymeleaf" "Web Application"

            caseService = container "Case Service" "Central orchestration service. For petition059 it owns the limitation-objection workflow lifecycle behind an internal API consumed by debt-service." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service" {
                properties {
                    "domain.concepts" "indsigelse, foraeldelse, fordring"
                }

                limitationObjectionWorkflowInternalApi = component "LimitationObjectionWorkflowInternalApi" "Internal workflow API for registering and evaluating limitation objections." "REST API" "Component"
                limitationObjectionWorkflow = component "LimitationObjectionWorkflow" "Flowable-managed objection workflow that tracks registration, review, decision, and completion." "Flowable BPM / orchestration" "Component"
                objectionAuditPublisher = component "ObjectionAuditPublisher" "Publishes workflow audit events through the shared audit pipeline." "Audit publisher" "Component"
            }

            creditorService = container "Creditor Service" "Creditor and master data management. Maintains creditor profiles, agreements, and claim submission rules." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service" {
                creditorController = component "CreditorController" "Internal creditor lookup surface. For petition026 it returns displayName together with creditor data for creditorOrgId-based resolution." "REST API" "Component"
            }

            debtService = container "Debt Service" "Fordring management. For petition059 it additionally owns the authoritative limitation state and the external limitation surface required by the petition/outcome contract." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service" {
                properties {
                    "domain.concepts" "fordring, foraeldelse, fordringskompleks, modregning"
                }

                citizenDebtController = component "CitizenDebtController" "External REST surface for GET /api/v1/citizen/debts." "REST API" "Component"
                citizenDebtService = component "CitizenDebtService" "Application service assembling the citizen debt projection, including creditor display names and citizen-safe status mapping." "Application service" "Component"
                creditorDisplayClient = component "CreditorDisplayClient" "Internal client resolving creditor display names from creditor-service by creditorOrgId." "HTTP client" "Component"
                businessConfigService = component "BusinessConfigService" "Resolves effective versioned interest-rate metadata for the citizen overview note." "Application service" "Component"
                citizenDebtPresentationMapper = component "CitizenDebtPresentationMapper" "Maps internal debt status, lifecycle, write-off, and paused-interest facts into citizen-safe enums and reason codes." "Presentation mapper" "Component"

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
                section50WorklistApi = component "Section50WorklistApi" "External REST surface for generating, reading, overriding, and annotating section-50 retskraft worklists." "REST API" "Component"
                section50WorklistApplicationService = component "Section50WorklistApplicationService" "Application seam for request validation, context assembly, persistence, and result composition for petition060." "Application service" "Component"
                section50OrderingPolicyEngine = component "Section50OrderingPolicyEngine" "Deterministic section-50 legal ordering engine for default, discretionary, expedited, and modregning paths." "Domain service" "Component"
                accessoryEligibilityEvaluator = component "AccessoryEligibilityEvaluator" "Determines whether accessory items are deferred, eligible, or excluded based on principal state and proportionality." "Domain service" "Component"
                surplusWindowSelector = component "SurplusWindowSelector" "Calculates remaining amount windows and candidate sets for voluntary-payment and modregning contexts." "Domain service" "Component"
                paymentCoverageOrderClient = component "PaymentCoverageOrderClient" "Read-only internal client to payment-service simulation for petition057 ordering reuse." "HTTP client" "Component"
                section50AuditPublisher = component "Section50AuditPublisher" "Publishes section-50 ranking and deviation audit events through the shared audit pipeline." "Audit publisher" "Component"
                attachmentWorkflowApi = component "AttachmentWorkflowApi" "Internal/public debtor-scoped REST surface for attachment workflow create, dispatch, callback, withdraw, and read operations for petition066." "REST API" "Component"
                attachmentWorkflowApplicationService = component "AttachmentWorkflowApplicationService" "Application service orchestrating attachment workflow validation, transaction boundaries, and result assembly." "Application service" "Component"
                attachmentEligibilityGate = component "AttachmentEligibilityGate" "Evaluates covered claims for attachment-workflow eligibility and returns per-claim rejection reasons." "Domain service" "Component"
                attachmentDispatchCoordinator = component "AttachmentDispatchCoordinator" "Coordinates idempotent fogedret dispatch and stores workflow dispatch metadata including workflowReference." "Application service" "Component"
                attachmentCallbackValidator = component "AttachmentCallbackValidator" "Validates debtor scope, workflowReference correlation, legal state transitions, and terminal idempotency for attachment callbacks." "Domain service" "Component"
                attachmentInterruptionBridge = component "AttachmentInterruptionBridge" "Registers petition059 UDLAEG interruption effects for terminal attachment outcomes with correct complex grouping and legal metadata." "Application service" "Component"
                attachmentWorkflowHistoryProjector = component "AttachmentWorkflowHistoryProjector" "Builds debtor-scoped attachment workflow read models with chronological status history and interruption linkage metadata." "Projection component" "Component"
            }

            letterService = container "Letter Service" "Document generation and delivery. Produces legally required notices, decisions, and correspondence." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service"

            paymentService = container "Payment Service" "Payment processing, reconciliation, and GIL § 4 payment application order (dækningsrækkefølge). Tracks incoming payments, applies them sequentially by PrioritetKategori and FIFO sort key, and posts to financial audit ledger. Owns DaekningRecord persistence and immudb audit appends (P057)." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service" {
                daekningsRaekkefoelgeSimulationApi = component "DaekningsRaekkefoelgeSimulationApi" "Simulation API returning principal ordering for a candidate set without applying payment." "REST API" "Component"
            }

            wageGarnishmentService = container "Wage Garnishment Service" "Lønindeholdelse processing. Petition059 uses it as the source of decision, notification, covered-claim, and inactivity facts needed to determine whether wage garnishment interrupts limitation periods." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service" {
                properties {
                    "domain.concepts" "loenindeholdelse, underretning"
                }

                limitationFactApi = component "LimitationFactApi" "Fact-oriented internal API for petition059. Exposes the minimum wage-garnishment state required by debt-service." "REST API" "Component"
            }

            rulesEngine = container "Rules Engine" "Business rule evaluation. Evaluates case eligibility, enforcement thresholds, interest rules, and priority logic using Drools." "Java 21 / Spring Boot 3.3, Drools" "Service"

            integrationGateway = container "Integration Gateway" "SOAP/EDIFACT legacy protocol adapter. Translates between OpenDebt's internal REST APIs and external systems using legacy protocols (SOAP, EDIFACT). Built on Apache Camel." "Java 21 / Spring Boot 3.3, Apache Camel" "Service" {
                fogedretCallbackController = component "FogedretCallbackController" "External callback ingress controller for petition066 fogedret attachment callbacks under OCES3 mTLS." "REST API" "Component"
                fogedretReplayGuard = component "FogedretReplayGuard" "Transport-layer replay protection for petition066 callback identity tuples before forwarding to debt-service." "Gateway guard" "Component"
                attachmentGatewayClient = component "AttachmentGatewayClient" "Internal client forwarding validated attachment dispatch and callback payloads to debtor-scoped debt-service APIs." "HTTP client" "Component"
            }

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
        citizenPortal -> debtService "Reads the citizen debt overview via" "HTTPS/REST"
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
        debtService -> creditorService "Resolves creditor display names for the citizen projection via" "HTTPS/REST"
        debtService -> personRegistry "Resolves person data by UUID" "HTTPS/REST"
        debtService -> paymentService "Delegates tier-2 partial allocation to DaekningsRaekkefoeigenService (P057, ADR-0007)" "HTTPS/REST"
        debtService -> paymentService "Posts double-entry ledger entries via LedgerServiceClient (ADR-0018)" "HTTPS/REST"
        debtService -> immudb "Appends ModregningEvent and SET_OFF CollectionMeasure records (post-commit, ADR-0029)" "gRPC"
        debtService -> letterService "Sends notification outbox events to (Transactional Outbox, ADR-0019)" "Async/Outbox"
        debtService -> wageGarnishmentService "Reads wage-garnishment decision and inactivity facts for limitation interruption rules" "HTTPS/REST"
        debtService -> keycloak "Validates tokens via" "OAuth2/OIDC"
        debtService -> cls "Ships limitation audit events via audit-trail-commons / ADR-0022 pipeline" "Structured audit pipeline"

        paymentService -> personRegistry "Resolves person data by UUID" "HTTPS/REST"
        paymentService -> keycloak "Validates tokens via" "OAuth2/OIDC"
        letterService -> personRegistry "Resolves person data for addressing" "HTTPS/REST"
        wageGarnishmentService -> personRegistry "Resolves person data by UUID" "HTTPS/REST"
        wageGarnishmentService -> keycloak "Validates tokens via" "OAuth2/OIDC"
        creditorService -> personRegistry "Resolves person data by UUID" "HTTPS/REST"
        paymentService -> immudb "Appends payment audit events" "gRPC"

        // ---------------------------------------------------------------
        // Relationships — Integration Gateway to External Systems
        // ---------------------------------------------------------------
        integrationGateway -> dupla "Submits enforcement orders and petition066 attachment dispatch traffic" "SOAP/HTTPS"
        integrationGateway -> psrm "Exchanges debt data" "SOAP/HTTPS"
        integrationGateway -> edifact "Receives creditor batch submissions" "EDIFACT/SFTP"
        caseService -> integrationGateway "Routes external integrations through" "HTTPS/REST"
        integrationGateway -> nemkonto "Receives disbursement interception events from" "HTTPS/Event"
        integrationGateway -> debtService "Forwards PublicDisbursementEvent for modregning processing" "Async/Messaging"
        integrationGateway -> debtService "Forwards validated petition066 attachment callbacks to debtor-scoped workflow APIs" "HTTPS/REST"

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
        // Component relationships — P026
        // ---------------------------------------------------------------
        debtOverviewController -> citizenDebtClient "Requests the citizen debt summary through" "Java method call"
        debtOverviewController -> debtOverviewView "Renders the debt overview page through" "Java method call"
        citizenDebtClient -> citizenDebtController "Calls the citizen debt summary contract on" "HTTPS/REST"
        citizenDebtController -> citizenDebtService "Delegates citizen debt projection assembly to" "Java method call"
        citizenDebtService -> creditorDisplayClient "Resolves creditor display names through" "Java method call"
        citizenDebtService -> businessConfigService "Resolves effective interest-rate metadata through" "Java method call"
        citizenDebtService -> citizenDebtPresentationMapper "Maps internal debt facts to citizen-safe view fields through" "Java method call"
        creditorDisplayClient -> creditorController "Calls creditor lookup for display-name enrichment on" "HTTPS/REST"

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
        // Component relationships — P066
        // ---------------------------------------------------------------
        attachmentWorkflowApi -> attachmentWorkflowApplicationService "Delegates attachment workflow commands and reads to" "Java method call"
        attachmentWorkflowApplicationService -> attachmentEligibilityGate "Evaluates covered-claim eligibility through" "Java method call"
        attachmentWorkflowApplicationService -> attachmentDispatchCoordinator "Delegates accepted dispatch handling to" "Java method call"
        attachmentWorkflowApplicationService -> attachmentCallbackValidator "Validates callback correlation and legal transitions through" "Java method call"
        attachmentWorkflowApplicationService -> attachmentInterruptionBridge "Registers petition059 interruption effects through" "Java method call"
        attachmentWorkflowApplicationService -> attachmentWorkflowHistoryProjector "Builds debtor-scoped status history and linkage responses through" "Java method call"
        attachmentInterruptionBridge -> limitationStateApplicationService "Registers UDLAEG interruption effects on petition059 limitation state through" "Java method call"
        attachmentDispatchCoordinator -> attachmentGatewayClient "Sends idempotent fogedret dispatch requests through" "Java method call"
        fogedretCallbackController -> fogedretReplayGuard "Checks callback replay identity through" "Java method call"
        fogedretCallbackController -> attachmentGatewayClient "Forwards validated fogedret callback payloads through" "Java method call"
        fogedretReplayGuard -> cls "Ships replay rejection and transport audit evidence to" "Structured audit pipeline"
        attachmentGatewayClient -> attachmentWorkflowApi "Calls debtor-scoped attachment workflow APIs on" "HTTPS/REST"
        attachmentGatewayClient -> dupla "Submits petition066 attachment dispatch traffic to" "SOAP/HTTPS"

        // ---------------------------------------------------------------
        // Component relationships — P060
        // ---------------------------------------------------------------
        section50WorklistController -> section50PortalClient "Uses for section-50 reads and actions" "Java method call"
        section50WorklistController -> section50WorklistView "Renders section-50 worklist screen" "Java method call"
        section50PortalClient -> section50WorklistApi "Calls debt-service section-50 surface" "HTTPS/REST"
        section50WorklistApi -> section50WorklistApplicationService "Delegates generation and decision commands to" "Java method call"
        section50WorklistApplicationService -> section50OrderingPolicyEngine "Delegates ordering logic to" "Java method call"
        section50WorklistApplicationService -> accessoryEligibilityEvaluator "Delegates accessory gating to" "Java method call"
        section50WorklistApplicationService -> surplusWindowSelector "Derives remaining amount windows through" "Java method call"
        section50WorklistApplicationService -> paymentCoverageOrderClient "Requests petition057 principal ordering through" "Java method call"
        section50WorklistApplicationService -> section50AuditPublisher "Emits ranking and deviation events through" "Java method call"
        paymentCoverageOrderClient -> daekningsRaekkefoelgeSimulationApi "Calls payment-service simulation contract" "HTTPS/REST"
        modregningService -> section50WorklistApplicationService "Supplies modregning-context remaining amount and confirmed-claim facts to" "Java method call"
        section50AuditPublisher -> cls "Ships section-50 ranking and deviation audit events to" "Structured audit pipeline"

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

        component debtService "DebtService_P026_Components" "P026 component view — citizen debt overview projection, creditor display-name enrichment, and page-rate metadata." {
            include debtOverviewController
            include citizenDebtClient
            include debtOverviewView
            include citizenDebtController
            include citizenDebtService
            include creditorDisplayClient
            include businessConfigService
            include citizenDebtPresentationMapper
            include creditorController
            include citizenPortal
            include creditorService
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

        component debtService "DebtService_P060_Components" "P060 component view — section-50 retskraft ordering, payment-service simulation reuse, and audit relations." {
            include section50WorklistController
            include section50WorklistView
            include section50PortalClient
            include modregningService
            include section50WorklistApi
            include section50WorklistApplicationService
            include section50OrderingPolicyEngine
            include accessoryEligibilityEvaluator
            include surplusWindowSelector
            include paymentCoverageOrderClient
            include section50AuditPublisher
            include daekningsRaekkefoelgeSimulationApi
            include caseworkerPortal
            include paymentService
            include cls
            autoLayout lr
        }

        component debtService "DebtService_P066_Components" "P066 component view — attachment workflow ownership, gateway callback boundary, petition059 interruption coupling, and replay protection." {
            include attachmentWorkflowApi
            include attachmentWorkflowApplicationService
            include attachmentEligibilityGate
            include attachmentDispatchCoordinator
            include attachmentCallbackValidator
            include attachmentInterruptionBridge
            include attachmentWorkflowHistoryProjector
            include limitationStateApplicationService
            include fogedretCallbackController
            include fogedretReplayGuard
            include attachmentGatewayClient
            include caseworkerPortal
            include integrationGateway
            include debtService
            include dupla
            include cls
            autoLayout lr
        }

        theme default
    }

    configuration {
        scope "softwareSystem"
    }
}
