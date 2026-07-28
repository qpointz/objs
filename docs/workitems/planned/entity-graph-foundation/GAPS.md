# Gaps & clarifications — entity-graph-foundation

Open decisions that affect implementation of
[`STORY.md`](STORY.md). Resolve here or in design docs before / during the listed WIs.
Do not invent silently in code without updating this file or [`docs/design/graph/`](../../../design/graph/).

**Blocking gaps for foundation:** none remaining (G-1–G-13, G-19, G-20 resolved).  
**Still open:** half-open / deferred items G-14–G-18 only (and WI-time DDL details).

**Legend:** `blocking` = prefer decide before that WI · `default-ok` = sensible default if unset · `half-open` = intentional defer

---

## Before WI-002 / WI-003 (domain + subgraph)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-1 | **Annotation shape** | resolved | **Key-value map** (see resolution log) |
| G-2 | **Annotation filter matching** | resolved | Extensible matcher base + default **match-all** (see resolution log) |
| G-3 | **Edge inclusion in subgraph** | resolved | **Induced:** edge included iff **source** and **target** are both in the selected entity set |
| G-4 | **Java type names** | resolved | **`Bo` prefix:** `BoEntity`, `BoEdge` (see resolution log) |

## Before WI-004 / WI-005 (validation + persistence)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-5 | **Entity identity** | resolved | **UUID v7** everywhere (see resolution log) |
| G-6 | **Entity type + JSON Schema registry** | resolved | **In-memory**; later PG tables — refined by **G-8** (central **type+version** schemas) |
| G-7 | **Allowed-edge rule shape** | resolved | `(sourceType, role, targetType)` + **properties policy** — see **G-7 detail** |
| G-8 | **Edge properties + schema mgmt** | resolved | Central **type+version** schemas; bare edges allowed per G-7 policy — see **G-8 detail** |
| G-9 | **Update / delete at persist** | resolved | **Same validation gate** for create, update, delete (see resolution log) |
| G-10 | **Schema ownership** | resolved | **Flyway from day one** (see resolution log) |
| G-11 | **Test database** | resolved | **H2** for WI-005 tests (see resolution log) |

## Process / repo

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-12 | **Branch base** | resolved | Create / use local **`dev`** as story base (see resolution log) |
| G-13 | **Maven group / artifacts** | resolved (default) | Group `org.poc.objs`; artifacts remain `objs-core` / `objs-service` |
| G-19 | **Batch subgraph persist** | resolved | One payload = entities + edges; two-stage validation — see **G-19 detail** |
| G-20 | **Create vs update by id** | resolved | **No id → create**; **id present → update** — see **G-20 detail** |

## G-7 detail — allowed-edge rule shape

**Status:** resolved (2026-07-28), amended for properties policy

| Decision | Choice |
|----------|--------|
| Rule identity | `(sourceType, role, targetType)` |
| Direction | **Directed** — `A --role--> B` ≠ `B --role--> A` unless both rules exist |
| Cardinality | **Unlimited** for now |
| Role | **Free string** |
| Catalog | **In-memory** allow-list; later PostgreSQL (C-3) |
| Policy | In catalog → continue checks; not in catalog → **deny** |
| **Properties policy** (per rule) | Declared on the allow-list entry — see below |

### Properties policy on the allow-list rule

Each permitted `(sourceType, role, targetType)` also states how **properties** behave for that role:

| Mode | Meaning |
|------|---------|
| **none** | Graph-theoretic **bare edge**: **no properties**. Persist must reject non-empty properties. No edge property `type+version` required. |
| **schema** | Edge has a JSON properties document validated via central schema **`(type, version)`**. Rule also states whether **empty** properties are **allowed** or **forbidden**. |

So: some roles are links only; others carry a validated payload. Empty-vs-not is a **policy** concern on the rule, not only a JSON Schema quirk.

## G-8 detail — edge properties and central schema management

**Status:** resolved (2026-07-28), amended with bare-edge caveat

| Decision | Choice |
|----------|--------|
| Edge properties | When policy = **schema**: JSON validated with **JSON Schema** at persist/audit |
| Bare edges | When policy = **none**: **no properties** (allowed and expected for that role) |
| Schema management | **Central repository** shared by **entities and edges** |
| Schema identity | **`type` + `version`** |
| Entity | Always **type + version**; payload validated against schema `(type, version)` |
| Edge | If properties policy = **schema**: edge carries **type + version** + properties; if **none**: omit / ignore property schema |
| Empty properties | Controlled by allow-list **properties policy** (`none` vs `schema` + empty allowed/forbidden); schema may further constrain |
| Storage of catalog (this story) | **In-memory**; later **PostgreSQL** (C-3) |

**Note:** G-7 allow-list decides *whether* properties exist for a role; G-8 decides *how* schemas are stored/selected when they do.

## G-19 detail — batch subgraph create / persist

**Status:** resolved (2026-07-28), amended with **two-stage validation**

Callers can persist a **set of entities and edges in one payload** (a writeable subgraph), not only single entities/edges.

| Decision | Choice |
|----------|--------|
| Write unit | One payload = **entities + edges** (subgraph-shaped) |
| Cross-store edges | Supported — e.g. new entity in payload + edge to an **existing** persisted entity |
| Missing endpoint | If source/target id is neither in the payload nor found in the store → **reject** |
| Atomicity | Prefer **all-or-nothing** for the batch (document in WI-005 if transaction boundaries differ) |

### Two-stage validation (simplicity)

Validation of a batch write is **split into two stages**:

1. **Entity stage** — validate **only entities** in the payload against their JSON Schemas (`type + version`). No edge checks yet. Fail fast if any entity is schema-invalid.
2. **Edge stage (right before persistence)** — validate **edges** against allow-list / properties policy, resolving source/target types from entities **in the payload** and entities **already in the persistent store**. Runs immediately before the write is committed/flushed.

Rationale: keep entity schema checks separate from graph/referential edge checks; edge validation always sees a coherent view of “new + existing” entities.

## G-20 detail — create vs update by id presence

**Status:** resolved (2026-07-28)

For **entities and edges** in a write (including batch subgraph payload):

| Id on item | Operation |
|------------|-----------|
| **Absent** (null / not provided) | **Create** — assign **UUID v7** on persist |
| **Present** | **Update** — must refer to an existing persisted row; reject if unknown id |

- Same rule for both `BoEntity` and `BoEdge`.
- **Delete** remains an **explicit** operation (requires id); not inferred from payload shape.
- Batch payloads may mix creates and updates (some items with ids, some without).

## Intentionally deferred (half-open / out of story)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-14 | Edge annotations | half-open | Working assumption: edges **not** annotated |
| G-15 | REST API | out of scope | Backlog C-2 |
| G-16 | JSONB indexing strategy | half-open | Follow-up after WI-005 |
| G-17 | Soft-delete / versioning / consistency | half-open | Not required for foundation |
| G-18 | Audit validation report format | default-ok | Minimal ok / list-of-issues sufficient for WI-004 |

---

## Resolution log

| Gap | Decision | Date | Where recorded |
|-----|----------|------|----------------|
| G-1 | Annotations are a **key-value map** | 2026-07-28 | [`annotations-and-subgraphs.md`](../../../design/graph/annotations-and-subgraphs.md); this file |
| G-2 | Extensible matching: **base matcher** + default **match-all** (entity matches iff **all** filter key-value pairs are present on the entity) | 2026-07-28 | [`annotations-and-subgraphs.md`](../../../design/graph/annotations-and-subgraphs.md); WI-003 |
| G-3 | Subgraph edges are **induced**: include edge iff **source** and **target** are both in the selected entity set. Edge ends named **source** / **target** (not “endpoints”) | 2026-07-28 | [`annotations-and-subgraphs.md`](../../../design/graph/annotations-and-subgraphs.md); [`model.md`](../../../design/graph/model.md) |
| G-4 | Java types use **`Bo` prefix:** `BoEntity`, `BoEdge` (avoids `java.lang.Object` / JPA `@Entity` clash). Domain vocabulary remains entity / edge | 2026-07-28 | [`model.md`](../../../design/graph/model.md); WI-002 |
| G-5 | Identity is **UUID v7** for entities (and edges if they have ids) — same in memory and PostgreSQL; v7 preferred for **index locality** | 2026-07-28 | [`model.md`](../../../design/graph/model.md); [`persistence.md`](../../../design/graph/persistence.md) |
| G-6 | Entity type + JSON Schema **registry is in-memory** for this story; **later** persisted as **separate PostgreSQL tables** (follow-up) | 2026-07-28 | [`model.md`](../../../design/graph/model.md); WI-004 |
| G-7 | Allowed edge = `(sourceType, role, targetType)` + **properties policy** (`none` bare edge vs `schema` + empty allowed/forbidden); directed allow-list | 2026-07-28 | this file (G-7 detail); [`validation.md`](../../../design/graph/validation.md); WI-004 |
| G-8 | Edge properties JSON Schema via central **type+version** when policy=`schema`; **bare edges** when policy=`none` | 2026-07-28 | this file (G-8 detail); [`model.md`](../../../design/graph/model.md); [`validation.md`](../../../design/graph/validation.md) |
| G-9 | Persist-time validation gate applies equally to **create, update, and delete** | 2026-07-28 | [`validation.md`](../../../design/graph/validation.md); WI-004 / WI-005 |
| G-10 | DB schema owned by **Flyway from day one** (not Hibernate `ddl-auto` as source of truth) | 2026-07-28 | [`persistence.md`](../../../design/graph/persistence.md); WI-005 |
| G-11 | Persistence tests use **H2** for this story (PostgreSQL remains the primary runtime DB) | 2026-07-28 | WI-005; [`persistence.md`](../../../design/graph/persistence.md) |
| G-19 | Persist **subgraph payload**; **two-stage** validation — (1) entities vs schema, (2) edges vs payload∪store right before persist | 2026-07-28 | this file (G-19 detail); [`validation.md`](../../../design/graph/validation.md); WI-004 / WI-005 |
| G-20 | **No id → create** (assign UUID v7); **id present → update** (must exist); delete is explicit | 2026-07-28 | this file (G-20 detail); [`validation.md`](../../../design/graph/validation.md); WI-002 / WI-005 |
| G-12 | Local **`dev`** branch is the integration / story base (create story branches from `dev`; use `origin/dev` once remote exists) | 2026-07-28 | this file; git `dev` |
| G-13 | Maven **group** `org.poc.objs`; artifact ids **`objs-core`** / **`objs-service`** unchanged | 2026-07-28 | WI-001 |
