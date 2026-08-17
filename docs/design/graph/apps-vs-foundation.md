# Object store: foundation vs example apps

**Status:** design note (2026-08-16), from SBOM inventory + asset repository  
**Audience:** objs-core / graph APIs  
**Not this doc:** product journeys, workbench REST (`objs-service`), Gradle/UI packaging (appendix only)

Both examples sit on **objs-core** (pool, named graphs, schemas, matchers, seeds, Gremlin `selectAndEval`).
They do **not** use foundation workbench REST as the app contract. Domain tables point at `graph_id`;
the store does not know “application version” vs “collection”.

SBOM story tracker for the same gaps: [`FOUNDATION-BACKLOG.md`](../../workitems/completed/20260816-sbom-inventory-app/FOUNDATION-BACKLOG.md) (`FB-*`). This note is the **cross-example** view.

## Layering

| Layer | Owns |
|-------|------|
| **objs-core** | Entities, edges, named graphs + membership, schema/allow-list catalogs, persist gate, identity *projection*, matchers (`obj-expr`, `graphs-in`, …), Graph/ObjectSchema/AllowedEdge seeds, Gremlin materialize + eval |
| **objs-gremlin-core** | Matcher → TinkerGraph → gremlin-lang |
| **Example apps** | Product tables and meaning of graphs (SBOM apps/versions/fingerprints/portfolios; AR collections + write modes), product REST, ontology packs, reports |
| **objs-service / objs-service-app** | Foundation *workbench* only — not an example dependency |

A third example should be mostly **domain table + routes + seeds**, not another copy of pool/graph query glue.

## What the store already does

- Global **entity pool** (type + version + JSON payload + annotations).
- **Named graphs**: header + membership + induced edges; select via matchers.
- **Catalog:** `(type, version)` schemas; allowed `(sourceType, role, targetType)` (+ cardinality / properties policy).
- **Writes:** batch persist gate (entities then edges); identity fields projected, not queried.
- **Reads:** forward select (graphs/entities matching matcher); Gremlin over a selected subgraph.
- **Seeds:** `ObjectSchema`, `AllowedEdgeRule`, `Graph` (and SPI for extra kinds — kinds themselves stay in apps).

## Missing in objs-* (object / graph)

These showed up in both products (SBOM named `FB-1`–`FB-3`; AR will hit the same if it grows “where used” / duplicates / typed search).

| Need | Why it is store, not product | Today |
|------|------------------------------|--------|
| **Reverse: entity → graphs + incident edges** | Shared objects; “who uses this?”; MI-style reports | App scans known `graph_id`s (SBOM drafts/versions) |
| **Find-by-identity / duplicate groups** | Identity projection exists; query does not | `selectFromPool` by type + group maps in memory |
| **Searchable-field matcher pushdown** | Schema `searchable` + `obj-expr` should be the fast path | Incomplete operators → slow path |
| **Paged pool select** | Inventory UIs page a type | Full select then sort/slice in the app |
| **Per-type counts without a full scan** | Schema portal / type lists | Scan or N queries |
| **Copy / clone graph membership** | Version snapshot, fingerprint, collection snapshot | Hand-rolled copy in the app (e.g. SBOM version capture) |
| **Schema catalog helpers** (latest per type, identifier/searchable field hints) | Same DTO both apps need | SBOM `AssetTypeCatalogService` vs AR `SchemaQueryService` |

Suggested store APIs (names indicative):

- `listGraphIdsForEntity(entityId)` / `listIncidentEdges(entityId)` — **FB-1**
- `findEntitiesByIdentity(type, identityMap)` / `findDuplicateGroups(type)` — **FB-2**
- Extend matcher pushdown on searchable paths — **FB-3**
- `selectFromPool(type, matcher, page)` with store-side limit/offset
- `countByType()` (pool, optional graph-scoped)
- `copyGraph(sourceGraphId) → newGraphId` (header + membership + edges; new graph id)
- Catalog: list ENTITY types, latest version, identifier vs searchable paths (no “used in apps”)

**Not missing as foundation:** applications, portfolios, collections-as-product, CycloneDX, MI *report definitions*, uniqueness of “app in portfolio”. Those stay domain.

## Shift from app → foundation (object / graph)

High value — same job in both examples:

1. Schema catalog listing + field hints (usage-in-product stays domain).
2. Pool create/update with **identifier immutability** (store policy, not SBOM-specific).
3. Display label from payload (`name` / first identifier).
4. Filter form → `obj-expr` for searchable paths.
5. Graph copy (new graph id, same members/edges).

Keep in apps:

- Which graphs mean what (version vs collection vs fingerprint).
- Product REST and transactional *product* Save (compose several store calls).
- Seed *kinds* for domain rows (`Collection`, `Portfolio`, `Application`) — handler SPI is already core.
- Relation *copy* / labels (`depends on`, …).

## Duplicates that belong in the store

| Duplicate in examples | Foundation shape |
|-----------------------|------------------|
| Schema catalog + identifier/searchable hints | Helpers on `BoMSchemaCatalog` (or a small catalog service in core) |
| In-memory “where used” / duplicate grouping / type stats | Reverse lookup, identity query, counts |
| Identity-immutable payload update | Persist/update rule in core |
| Graph clone for snapshots | `copyGraph` on `BoMNamedGraphStore` |

SPA chrome, schema *forms*, Gradle node-gradle packaging are duplicates too; they are **not** store APIs (see appendix).

## Stay domain

- SBOM: application/version/fingerprint tables, portfolios, MI-1…4 *meaning*, CDX import/export, demo inventory seeder.
- AR: `ar_collection`, write modes, SPI extensions.
- Graph *membership* is already foundation; domain only stores `graph_id`.

## Appendix — UI and Gradle (secondary)

Not store work. Recorded so a later UI-kit / boot-starter story does not confuse them with FB-1/2/3.

**URL = classpath folder** under `static/`:

| App | URL | Classpath |
|-----|-----|-----------|
| Workbench | `/workbench/` | `static/workbench/` |
| Asset repository | `/ar/` | `static/ar/` |
| SBOM inventory | `/sbom/` | `static/sbom/` |

- Both SPAs copy schema portal/view/tree, instance forms, layout chrome.
- SPA deep-link filter: SBOM copies a filter; AR extends `objs-service` (examples must not depend on workbench REST).
- Vite → JAR via node-gradle is a build convention, not a graph capability.

Optional later: small **boot starter** (SPA filter, static UI) *without* workbench REST; **example UI kit** for Mantine schema browse/forms.
