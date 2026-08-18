# Canonical Software Graph Specification

**Version:** 1.0 Draft  
**Generated:** 2026-07-28  
**Status:** registered in [`objs-sbom-example`](../../workitems/completed/20260728-sbom-typed-example/STORY.md) via classpath seed YAML (`seeds/sbom-ontology.yaml`) with typed `SbomRegistry` parity — all object types and relationship triples below  
**Mapping to objs foundation:** see [`example.md`](example.md) and story [`GAPS.md`](../../workitems/completed/20260728-sbom-typed-example/GAPS.md)

## Purpose

A technology-neutral ontology for representing software systems. SBOMs, deployment inventories, architecture diagrams and security graphs are projections of this canonical graph.

## objs implementation

| Concern | Binding |
|---------|---------|
| Module | `objs-sbom-example` |
| Registry pack (typed / parity) | `org.poc.objs.sbom.registry.SbomRegistry.pack()` |
| Runtime ontology seed | `classpath:seeds/sbom-ontology.yaml` via [`../graph/seeds.md`](../graph/seeds.md); JSON Schema → seeds [`../graph/json-schema-to-seeds.md`](../graph/json-schema-to-seeds.md) |
| Graph type color | Entity envelope `attributes.color` (`#rrggbb` from the workbench palette, or `nocolor` for theme gray). See [`../graph/object-schema-dsl.md`](../graph/object-schema-dsl.md) |
| Schema version | `1.0.0` for every entity type and shared edge properties |
| Typed payloads | `org.poc.objs.sbom.model` (`WaveATypes`, `WaveBCDTypes`, `Component`) |
| Edge properties | Shared schema type `CanonicalEdge` → `BoMEdge.properties` (`SCHEMA`, empty allowed) |
| Roles | UPPER_SNAKE names in the relationship table (`SbomRoles`) |
| OpenAPI | Catalog schemas published as `{type}.{version}` (spaces preserved, e.g. `Container Image.1.0.0`) |

### Coverage by wave

| Wave | Types | Code |
|------|-------|------|
| A — SBOM / supply-chain | Product, Component, Organization, License, Vulnerability, Build | `WaveATypes.kt`, `Component.kt` |
| B — build & packaging | Source Repository, Source Module, Artifact, Container Image, Container Layer | `WaveBCDTypes.kt` |
| C — runtime & deploy | Runtime, Operating System, Deployment, Environment, Host, Kubernetes Cluster, Namespace | `WaveBCDTypes.kt` |
| D — architecture & compliance | Service, API, Database, Dataset, Policy | `WaveBCDTypes.kt` |

**Counts:** 23 entity schemas + `CanonicalEdge` + **28** allow-listed relationship triples (table at end of this doc). Multi-app BOM partition uses annotations (`app`, `appVersion`, …), not payload fields — see [`example.md`](example.md).

## Foundation object properties (canonical)

| Property | Type | Description |
|----------|------|-------------|
| id | UUID | Global object identifier |
| type | String | Object type |
| name | String | Display name |
| description | String | Human description |

## Foundation edge properties (canonical)

| Property | Type | Description |
|----------|------|-------------|
| id | UUID | Edge id |
| createdAt | Instant | Creation time |
| source | String | Discovery source |
| confidence | Decimal | Confidence 0..1 |

---

# Object specifications

Common notes for all types:

- **Semantic identity:** one logical entity independent of physical storage or runtime instances.
- **Identity rules:** object-specific natural key combined with version where applicable; importers should merge objects with identical identities *(merge behaviour is not implemented by the objs foundation in this story)*.
- **Enumerations:** use listed values where applicable; implementations may allow open strings for forward compatibility *(full value lists often still TBD)*.
- **Lifecycle:** created, versioned if applicable, immutable after publication, superseded by newer revisions.
- **`id`:** listed for human clarity; objs maps it to `BoMEntity.id` / `BoMEdge.id` (envelope), not duplicated as a required JSON-Schema payload field.

## Product

**Description:** Logical software product delivered to users.

**Examples:** Customer Portal, Billing API, Mobile App

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| version | SemVer | ✓ | Released version |
| supplier | String | | Owning organization |
| lifecycle | Enum | | Development, Active, Deprecated, Retired |
| homepage | URI | | Product home |
| description | String | | Human-readable description |

**Outgoing:** `CONTAINS` → Component; `CONTAINS` → Artifact; `RUNS_ON` → Runtime; `CALLS` → API; `CONNECTS_TO` → Database; `OWNED_BY` → Organization; `COMPLIES_WITH` → Policy

## Component

**Description:** Reusable software package.

**Examples:** Spring Boot, Jackson, OpenSSL

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| version | SemVer/String | ✓ | Published version |
| ecosystem | Enum | ✓ | Package ecosystem |
| kind | Enum | ✓ | library / framework / sdk / etc. |
| coordinates | String | | Package identifier |
| description | String | | Human-readable description |

**Outgoing:** `DEPENDS_ON` → Component; `PROVIDED_BY` → Organization; `LICENSED_UNDER` → License; `HAS_VULNERABILITY` → Vulnerability  

**Incoming:** `CONTAINS` ← Product; `DEPENDS_ON` ← Component; `USES` ← Build

## Organization

**Description:** Legal entity.

**Examples:** Apache Software Foundation, Microsoft

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| domain | String | | Primary domain |
| website | URI | | Website |
| country | String | | Country |
| description | String | | Human-readable description |

**Incoming:** `PROVIDED_BY` ← Component; `OWNED_BY` ← Product

## License

**Description:** Software license.

**Examples:** Apache-2.0, MIT

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| spdxId | String | ✓ | SPDX identifier |
| url | URI | | License URL |
| description | String | | Human-readable description |

**Incoming:** `LICENSED_UNDER` ← Component

## Vulnerability

**Description:** Known security issue.

**Examples:** CVE-2026-1234

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| cve | String | ✓ | CVE identifier |
| severity | Enum | ✓ | Critical / High / Medium / Low |
| cvss | Decimal | | CVSS score |
| description | String | | Human-readable description |

**Incoming:** `HAS_VULNERABILITY` ← Component; `HAS_VULNERABILITY` ← Container Image

## Build

**Description:** CI/CD execution.

**Examples:** GitHub Actions #42

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| buildNumber | String | ✓ | Build id |
| builder | String | | CI system |
| status | Enum | ✓ | Result |
| description | String | | Human-readable description |

**Outgoing:** `BUILDS` → Artifact; `BUILDS` → Container Image; `USES` → Component

## Source Repository

**Description:** Version control repository.

**Examples:** GitHub repo

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| url | URI | ✓ | Repository URL |
| revision | Git SHA | | Commit |
| branch | String | | Branch |
| description | String | | Human-readable description |

**Outgoing:** `CONTAINS` → Source Module

## Source Module

**Description:** Logical build module.

**Examples:** backend, ui

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| path | String | ✓ | Module path |
| language | Enum | | Primary language |
| description | String | | Human-readable description |

**Outgoing:** `PRODUCES` → Artifact  
**Incoming:** `CONTAINS` ← Source Repository

## Artifact

**Description:** Compiled output.

**Examples:** jar, dll, wheel

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| artifactType | Enum | ✓ | Artifact kind |
| checksum | SHA-256 | | Hash |
| size | Long | | Bytes |
| description | String | | Human-readable description |

**Incoming:** `CONTAINS` ← Product; `PRODUCES` ← Source Module; `BUILDS` ← Build; `PACKAGES` ← Container Image

## Container Image

**Description:** OCI image.

**Examples:** customer-api:1.2

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| tag | String | ✓ | Image tag |
| digest | SHA-256 | | OCI digest |
| registry | URI | | Registry |
| description | String | | Human-readable description |

**Outgoing:** `CONTAINS` → Container Layer; `PACKAGES` → Artifact; `BASED_ON` → Operating System; `HAS_VULNERABILITY` → Vulnerability  
**Incoming:** `BUILDS` ← Build; `DEPLOYS` ← Deployment

## Container Layer

**Description:** Single OCI layer.

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| digest | SHA-256 | ✓ | Layer digest |
| size | Long | | Bytes |
| description | String | | Human-readable description |

**Incoming:** `CONTAINS` ← Container Image

## Runtime

**Description:** Execution runtime.

**Examples:** JVM, NodeJS

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| runtimeType | Enum | ✓ | Runtime |
| version | String | | Version |
| description | String | | Human-readable description |

**Outgoing:** `RUNS_ON` → Operating System  
**Incoming:** `RUNS_ON` ← Product

## Operating System

**Description:** Operating system.

**Examples:** Ubuntu 24.04

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| distribution | String | ✓ | Distribution |
| version | String | | Version |
| architecture | String | | CPU arch |
| description | String | | Human-readable description |

**Incoming:** `BASED_ON` ← Container Image; `RUNS_ON` ← Runtime

## Deployment

**Description:** Running deployment.

**Examples:** prod deployment

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| status | Enum | | Running state |
| replicas | Integer | | Replica count |
| deployedAt | Instant | | Timestamp |
| description | String | | Human-readable description |

**Outgoing:** `DEPLOYS` → Container Image; `TARGETS` → Environment; `RUNS_ON` → Host; `LOCATED_IN` → Namespace

## Environment

**Description:** Deployment environment.

**Examples:** DEV, TEST, PROD

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| environment | Enum | ✓ | Environment |
| description | String | | Human-readable description |

**Incoming:** `TARGETS` ← Deployment

## Host

**Description:** VM or physical server.

**Examples:** vm-001

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| hostname | String | ✓ | Hostname |
| ip | IPAddress | | IP |
| provider | String | | Cloud/provider |
| description | String | | Human-readable description |

**Outgoing:** `MEMBER_OF` → Kubernetes Cluster  
**Incoming:** `RUNS_ON` ← Deployment

## Kubernetes Cluster

**Description:** Cluster.

**Examples:** prod-cluster

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| version | String | | K8s version |
| description | String | | Human-readable description |

**Incoming:** `MEMBER_OF` ← Host

## Namespace

**Description:** Kubernetes namespace.

**Examples:** payments

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| namespace | String | ✓ | Namespace |
| description | String | | Human-readable description |

**Incoming:** `LOCATED_IN` ← Deployment

## Service

**Description:** Logical runtime service.

**Examples:** Payment Service

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| protocol | Enum | | HTTP / gRPC |
| endpoint | URI | | Endpoint |
| description | String | | Human-readable description |

**Outgoing:** `IMPLEMENTS` → API

## API

**Description:** Published interface.

**Examples:** Customer REST API

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| protocol | Enum | ✓ | REST / gRPC / GraphQL |
| version | String | | API version |
| description | String | | Human-readable description |

**Incoming:** `IMPLEMENTS` ← Service; `CALLS` ← Product

## Database

**Description:** Persistent datastore.

**Examples:** PostgreSQL

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| engine | Enum | ✓ | Database engine |
| version | String | | Engine version |
| description | String | | Human-readable description |

**Outgoing:** `CONTAINS` → Dataset  
**Incoming:** `CONNECTS_TO` ← Product

## Dataset

**Description:** Logical collection of data.

**Examples:** customers table

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| datasetType | Enum | ✓ | Table / Bucket / Topic |
| classification | String | | Sensitivity |
| description | String | | Human-readable description |

**Incoming:** `CONTAINS` ← Database

## Policy

**Description:** Compliance or governance policy.

**Examples:** Secure Build Policy

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| id | UUID | ✓ | Graph object identifier |
| name | String | ✓ | Human-readable name |
| policyType | Enum | | Security / Compliance |
| version | String | | Policy version |
| description | String | | Human-readable description |

**Incoming:** `COMPLIES_WITH` ← Product

---

# Canonical relationships

Normative allow-list for `objs-sbom-example`: every row is registered in `SbomRegistry` (`BoMAllowedEdgeRule` with role = Relationship, source/target = Source/Target).

| Relationship | Source | Target | Cardinality | Meaning |
|--------------|--------|--------|-------------|---------|
| CONTAINS | Product | Component | `1:*` | Composition |
| CONTAINS | Product | Artifact | `1:*` | Composition |
| CONTAINS | Container Image | Container Layer | `1:*` | Composition |
| CONTAINS | Database | Dataset | `1:*` | Composition |
| CONTAINS | Source Repository | Source Module | `1:*` | Composition |
| DEPENDS_ON | Component | Component | `1:*` | Dependency |
| PRODUCES | Source Module | Artifact | `1:*` | Build |
| BUILDS | Build | Artifact | `1:*` | Build |
| BUILDS | Build | Container Image | `1:*` | Build |
| USES | Build | Component | `1:*` | Build |
| PACKAGES | Container Image | Artifact | `1:*` | Packaging |
| BASED_ON | Container Image | Operating System | `1:1` | Packaging |
| RUNS_ON | Product | Runtime | `1:1` | Runtime |
| RUNS_ON | Runtime | Operating System | `1:1` | Runtime |
| DEPLOYS | Deployment | Container Image | `1:1` | Deployment |
| TARGETS | Deployment | Environment | `1:1` | Deployment |
| RUNS_ON | Deployment | Host | `1:*` | Deployment |
| MEMBER_OF | Host | Kubernetes Cluster | `1:1` | Infrastructure |
| LOCATED_IN | Deployment | Namespace | `1:1` | Infrastructure |
| IMPLEMENTS | Service | API | `1:*` | Architecture |
| CALLS | Product | API | `1:*` | Architecture |
| CONNECTS_TO | Product | Database | `1:*` | Architecture |
| PROVIDED_BY | Component | Organization | `1:1` | Governance |
| OWNED_BY | Product | Organization | `1:1` | Governance |
| LICENSED_UNDER | Component | License | `1:*` | Governance |
| HAS_VULNERABILITY | Component | Vulnerability | `1:*` | Security |
| HAS_VULNERABILITY | Container Image | Vulnerability | `1:*` | Security |
| COMPLIES_WITH | Product | Policy | `1:*` | Compliance |
