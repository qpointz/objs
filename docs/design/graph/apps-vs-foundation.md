# Object store: foundation vs example apps

**Status:** design note (2026-08-16), from SBOM inventory + asset repository  
**Shipped:** [C-17 `live-store-apis`](../../workitems/completed/20260819-live-store-apis/STORY.md), [C-18 versions](../../workitems/completed/20260819-versions-and-snapshots/STORY.md) (clocks + HEAD/history + Snapshot freeze; `clone()` kept). Next: [C-19 after versions](../../workitems/planned/foundation-after-versions/STORY.md). Text `q`: [C-20 `store-text-search`](../../workitems/planned/store-text-search/STORY.md) (does not block C-18). Sequence: [`SEQUENCE.md`](../../workitems/SEQUENCE.md).  
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
- **Writes:** batch persist gate (entities then edges); identity fields projected and queried (`findEntitiesByIdentity` / `findDuplicateGroups`).
- **Reads:** forward select; reverse membership (`listGraphIdsForEntity` / `listIncidentEdges`); Gremlin over a selected subgraph; paged `selectFromPool` + `countByType`.
- **Live graph ops:** `copyGraph` (same pool ids), `mergeGraph` (`GraphMergePolicy`, default `FirstSeenGraphMergePolicy`), hard `clone()` (new ids).
- **Catalog helpers:** `BoMCatalogSupport` — latest entity schema, field hints, `allowedEdgesForType`, `displayLabel`, `filterMapToObjExpr`.
- **Seeds:** `ObjectSchema`, `AllowedEdgeRule`, `Graph` (and SPI for extra kinds — kinds themselves stay in apps).

## Missing in objs-* (object / graph)

C-17 shipped catalog helpers, reverse/identity, live `copyGraph`/`mergeGraph`, and paging. Remaining cross-app gaps:

| Need | Why it is store, not product | Today |
|------|------------------------------|--------|
| **Searchable-field matcher pushdown** | Schema `searchable` + `obj-expr` should be the fast path | Incomplete operators → slow path (contains/`q` = C-20; rest C-19) |
| **Text `q` on payload scalars** | Objects page, SBOM assets, AR collection objects all need substring search | Equality `obj-expr` or load-then-filter; graph **header** search is separate |
| **`createdAt` / `updatedAt` on graph, node, edge** | Every consumer wants last-changed; must not live in payload | Catalog already stamped. HEAD clocks are **C-18 WI-002** (not C-19) |
| **Versions + snapshots** | Fingerprints must not pollute the live pool or drift when HEAD moves | Freeze = **`createDeepGraphVersion`** (same `graph_id`, pins). **`clone()` stays** a new-id deep copy with an empty history line. **C-18** [`versions-and-snapshots`](../../workitems/completed/20260819-versions-and-snapshots/STORY.md) |

Shipped store APIs (C-17):

- `listGraphIdsForEntity(entityId)` / `listIncidentEdges(entityId, graphId?)` — **shipped** (C-17 WI-003 / FB-1)
- `findEntitiesByIdentity(type, identityMap)` / `findDuplicateGroups(type)` — **shipped** (C-17 WI-004 / FB-2)
- Extend matcher pushdown on searchable paths — **FB-3** (`q` / contains = [C-20](../../workitems/planned/store-text-search/STORY.md); other ops C-19)
- Pool/graph text `q` over identifier + searchable scalars — **C-20** (workbench Objects, SBOM assets, AR objects)
- `selectFromPool(matcher, page)` + `countByType()` — **shipped** (C-17 WI-006)
- Catalog helpers (`BoMCatalogSupport`) — **shipped** (C-17 WI-002)
- `copyGraph(sourceGraphId, annotations) → BoMResolvedGraph` — **shipped** (C-17 WI-005): one live graph, **same** pool entity ids, new edge ids
- `mergeGraph(sourceGraphIds, annotations, GraphMergePolicy) → BoMResolvedGraph` — **shipped** (C-17 WI-005): persist-union; default first-seen
- Store-managed `createdAt` / `updatedAt` on HEAD + version rows — **C-18** (not C-17, not C-19)
- Entity/edge versions + HEAD; Snapshot = `createDeepGraphVersion` (pins); `clone()` kept — **C-18** [`versions-and-snapshots`](../../workitems/completed/20260819-versions-and-snapshots/STORY.md), not C-17 |

## C-17 live graph copy vs merge vs clone

Lock for [`live-store-apis`](../../workitems/completed/20260819-live-store-apis/STORY.md) WI-005. Workbench Composer **`clone()` REST stays**; examples do not call `/api/v1/objs/**`.

| API | Sources | Pool entities | Edges | Policy | Callers |
|-----|---------|---------------|-------|--------|---------|
| `copyGraph(sourceId, annotations)` | exactly one | **same ids** (membership only) | copied, **new ids**, new `graphId` | none | SBOM **keep-split** new draft; AR **collection copy** |
| `mergeGraph(sourceIds, annotations, policy)` | 1..n | same ids; collisions via policy | copied, new ids; collisions via policy | `GraphMergePolicy` (default `FirstSeenGraphMergePolicy`) | SBOM **combine-on-new-draft** |
| `clone()` | one | **new ids** | new ids, remapped endpoints | n/a | Workbench Composer **Clone** (C-18: empty history on the new graph) |
| `createDeepGraphVersion` | one | **same ids** (pins) | same edge ids at pin versions | n/a | Composer **Create version**; SBOM fingerprint |

`GraphMergePolicy`: `nodeKey` / `edgeKey` detect overlap; `onDuplicateNode` / `onDuplicateEdge` choose the survivor. Default: node key = entity id; edge key = `(source, role, target)`; keep first in caller order; do not merge property maps. Empty `sourceIds` → `GRAPH_MERGE_EMPTY`; any missing source → `GRAPH_NOT_FOUND` and no new graph.

Do **not** overload `copyGraph` with a collection of ids. Combined SBOM **GET** / multi-select stays ephemeral `BomUnion` (not persist). Fingerprint freeze is **C-18 `createDeepGraphVersion`**, not copy, merge, or clone.

**Not missing as foundation:** applications, portfolios, collections-as-product, CycloneDX, MI *report definitions*, uniqueness of “app in portfolio”. Those stay domain.

## Shift from app → foundation (object / graph)

High value — same job in both examples:

1. Schema catalog listing + field hints (usage-in-product stays domain).
2. Pool create/update with **identifier immutability** (store policy, not SBOM-specific).
3. Display label from payload (`name` / first identifier).
4. Filter form → `obj-expr` for searchable paths.
5. Live graph copy (`copyGraph`) and persist-union (`mergeGraph`).

Keep in apps:

- Which graphs mean what (version vs collection vs fingerprint).
- Product REST and transactional *product* Save (compose several store calls).
- Seed *kinds* for domain rows (`Collection`, `Portfolio`, `Application`) — handler SPI is already core.
- Relation *copy* / labels (`depends on`, …).

## Duplicates that belong in the store

| Duplicate in examples | Foundation shape |
|-----------------------|------------------|
| Schema catalog + identifier/searchable hints | `BoMCatalogSupport` (latest schema, field hints, allow-list for type) |
| “Where used” / duplicate grouping / type stats | `listGraphIdsForEntity`, `findDuplicateGroups`, `countByType` |
| Identity-immutable payload update | Persist/update rule in core (`IDENTIFIER_IMMUTABLE`) |
| Graph freeze for fingerprints | C-18 `createDeepGraphVersion` (not `clone()`, not C-17 `copyGraph`). Live share = `copyGraph`; persist-union = `mergeGraph`; independent duplicate = `clone()` |

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
