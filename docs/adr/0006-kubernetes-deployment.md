# ADR 0006: Kubernetes Deployment on Horizontale Driftsplatform

## Status
Accepted

## Context
UFST's Horizontale Driftsplatform is a shared Kubernetes-based platform for hosting applications. It provides:
- Managed Kubernetes clusters
- Shared Keycloak for authentication
- Ingress controllers
- Monitoring stack (Prometheus/Grafana)
- Logging infrastructure
- GitOps deployment via ArgoCD

## Decision
We deploy all OpenDebt services to the Horizontale Driftsplatform using:

### Container Strategy
- One container per microservice
- Base image: Eclipse Temurin JRE 21
- Non-root user execution
- Read-only filesystem where possible

### Kubernetes Resources
- **Deployments**: Stateless services with replicas
- **Services**: ClusterIP for internal communication
- **Ingress**: Via platform ingress controller
- **ConfigMaps**: Non-sensitive configuration
- **Secrets**: Sensitive data (managed externally)
- **ServiceAccount**: Per-service accounts with minimal RBAC

### Configuration Management
- **Kustomize**: For environment-specific overlays
- **Overlays**: `staging`, `production`
- **External secrets**: Via platform secret management

### Resource Allocation
| Environment | Replicas | Memory Request | Memory Limit |
|-------------|----------|----------------|--------------|
| Staging     | 1        | 512Mi          | 1Gi          |
| Production  | 3        | 1Gi            | 2Gi          |

### Health Checks
- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`
- Startup probe for slow-starting services

### Security
- Pod Security Standards: Restricted
- Network Policies: Service-to-service only
- No privileged containers
- Secrets mounted as files, not environment variables

## Consequences

### Positive
- Platform-managed infrastructure
- Automatic scaling and healing
- Standardized deployment process
- Integrated monitoring and logging

### Negative
- Platform dependency
- Shared resource constraints
- Learning curve for Kubernetes

### Mitigations
- Local development with Docker Compose
- Comprehensive documentation
- Platform team support
