# Canonical Software Graph Specification

**Version:** 1.0 Draft  
**Generated:** 2026-07-28  
**Status:** registered in [`objs-sbom-example`](../../workitems/completed/20260728-sbom-typed-example/STORY.md) (`SbomRegistry`) — all object types and relationship triples below  
**Mapping to objs foundation:** see [`example.md`](example.md) and story [`GAPS.md`](../../workitems/completed/20260728-sbom-typed-example/GAPS.md)

## Purpose

A technology-neutral ontology for representing software systems. SBOMs, deployment inventories, architecture diagrams and security graphs are projections of this canonical graph.

## objs implementation

| Concern | Binding |
|---------|---------|
| Module | `objs-sbom-example` |
| Registry pack | `org.poc.objs.sbom.registry.SbomRegistry.pack()` |
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
| labels | List\<String\> | Tags |
| attributes | Map\<String, Object\> | Extensions |

## Foundation edge properties (canonical)

| Property | Type | Description |
|----------|------|-------------|
| id | UUID | Edge id |
| createdAt | Instant | Creation time |
| source | String | Discovery source |
| confidence | Decimal | Confidence 0..1 |
| attributes | Map\<String, Object\> | Extensions |

---

# Object specifications

Common notes for all types:

- **Semantic identity:** one logical entity independent of physical storage or runtime instances.
- **Identity rules:** object-specific natural key combined with version where applicable; importers should merge objects with identical identities *(merge behaviour is not implemented by the objs foundation in this story)*.
- **Enumerations:** use listed values where applicable; implementations may allow open strings for forward compatibility *(full value lists often still TBD)*.
- **Lifecycle:** created, versioned if applicable, immutable after publication, superseded by newer revisions.
- **Extensions:** additional domain-specific attributes may be stored in `attributes` without changing the canonical schema.
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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

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
| labels | List\<String\> | | User tags |
| attributes | Map\<String, Object\> | | Custom extension properties |

**Incoming:** `COMPLIES_WITH` ← Product

---

# Canonical relationships

Normative allow-list for `objs-sbom-example`: every row is registered in `SbomRegistry` (`BoMAllowedEdgeRule` with role = Relationship, source/target = Source/Target).

| Relationship | Source | Target | Meaning |
|--------------|--------|--------|---------|
| CONTAINS | Product | Component | Composition |
| CONTAINS | Product | Artifact | Composition |
| CONTAINS | Container Image | Container Layer | Composition |
| CONTAINS | Database | Dataset | Composition |
| CONTAINS | Source Repository | Source Module | Composition |
| DEPENDS_ON | Component | Component | Dependency |
| PRODUCES | Source Module | Artifact | Build |
| BUILDS | Build | Artifact | Build |
| BUILDS | Build | Container Image | Build |
| USES | Build | Component | Build |
| PACKAGES | Container Image | Artifact | Packaging |
| BASED_ON | Container Image | Operating System | Packaging |
| RUNS_ON | Product | Runtime | Runtime |
| RUNS_ON | Runtime | Operating System | Runtime |
| DEPLOYS | Deployment | Container Image | Deployment |
| TARGETS | Deployment | Environment | Deployment |
| RUNS_ON | Deployment | Host | Deployment |
| MEMBER_OF | Host | Kubernetes Cluster | Infrastructure |
| LOCATED_IN | Deployment | Namespace | Infrastructure |
| IMPLEMENTS | Service | API | Architecture |
| CALLS | Product | API | Architecture |
| CONNECTS_TO | Product | Database | Architecture |
| PROVIDED_BY | Component | Organization | Governance |
| OWNED_BY | Product | Organization | Governance |
| LICENSED_UNDER | Component | License | Governance |
| HAS_VULNERABILITY | Component | Vulnerability | Security |
| HAS_VULNERABILITY | Container Image | Vulnerability | Security |
| COMPLIES_WITH | Product | Policy | Compliance |
