# Example rewiring — live-store-apis (C-17)

**Normative** with [`STORY.md`](STORY.md). Every **feature** WI shipped core **and** rewired **every labeled consumer** in the same commit. Product meaning stays in the example (applications, collections, reports). Workbench analysis: [`WORKBENCH.md`](WORKBENCH.md).

Labels on WIs:

| Label | Meaning |
|-------|---------|
| **docs** | Design/trackers only — no example code |
| **SBOM + AR** | Both product backends must call the new core API |
| **workbench + SBOM + AR** | Store-wide: workbench REST/UI **and** both examples |

---

## Rewiring (C-17 done)

Feature WIs WI-002…006 are complete. The **Now** column is the core API the consumer calls. Remaining rows are **C-20**, **C-19**, or domain-owned.

### SBOM (`:sbom-service`)

| Now (core API) | Replaced stopgap | WI |
|----------------|------------------|----|
| `BoMCatalogSupport` latest schema + field hints | Homegrown latest-per-type + field hints in `AssetTypeCatalogService` | WI-002 |
| `allowedEdgesForType` | Wildcard scan in `SchemaBrowseService` | WI-002 |
| `displayLabel` + `filterMapToObjExpr` | App-side label / filter → `obj-expr` | WI-002 |
| `listGraphIdsForEntity` + `listIncidentEdges` | Usage scan of draft/version graphs | WI-003 |
| Reverse lookup, then domain map graph→app | `MiReportService` MI-2/MI-3 membership intersect | WI-003 |
| `findDuplicateGroups` / `findEntitiesByIdentity` | In-memory duplicate groups / MI-4 | WI-004 |
| Persist gate only (`IDENTIFIER_IMMUTABLE`) | Identifier freeze in `update` | WI-004 |
| `BoMNamedGraphStore.copyGraph` (not fingerprint) | Keep-split new-draft copy | WI-005 |
| `BoMNamedGraphStore.mergeGraph` (default first-seen) | Combine-on-new-draft (`BomUnion` + `materialize`) | WI-005 |
| Paged `selectFromPool` + `countByType` | Full select then slice in `searchPage` / `statistics` | WI-006 |
| Combined SBOM GET / multi-select union | **Stays ephemeral `BomUnion`** (do not persist) | — |
| Store text `q` | Asset search equality-only / in-memory | **C-20** |
| Application `LIKE` on `sbom_application` | **Stays domain** (not pool FTS) | — |
| Store clocks on assets/relations | **C-19** after versions | — |

### AR (`:asset-repository-service`)

| Now (core API) | Replaced stopgap | WI |
|----------------|------------------|----|
| Catalog latest + field hints | Homegrown latest + catalog DTO in `SchemaQueryService` | WI-002 |
| `allowedEdgesForType` | Local allow-list scan | WI-002 |
| `filterMapToObjExpr` | Filter map → `obj-expr` in `ObjectWriteService` | WI-002 |
| `listIncidentEdges(entityId, collection.graphId)` | Full-graph edge walk in `listRelations` | WI-003 |
| `findEntitiesByIdentity` then keep hits in this graph | Scan collection members in `findByIdentity` | WI-004 |
| Live `copyGraph` + new `ar_collection` row | No collection copy | WI-005 |
| `mergeGraph` | **No AR consumer** this story (core tests only) | WI-005 |
| Graph-scoped `countByType` / paged `listMembers` | Load full graph for `objectCount`; unpaged object list | WI-006 |
| Store text `q` graph-scoped | Object search via equality `obj-expr` / full graph | **C-20** |
| Collection name `LIKE` | **Stays domain** | — |
| Collection last-updated unused | **C-19** | — |

### Workbench (`:objs-service` / UI)

| Now | Replaced / remaining | WI |
|-----|----------------------|----|
| Core `allowedEdgesForType` | In-controller `edgesForType` | WI-002 |
| Paged `selectFromPool` (`POST /entities/query?page=&size=`) | Unbounded pool query | WI-006 |
| Text `q` / contains on searchable fields | Objects matcher = equality `obj-expr` only | **C-20** |
| Schema explorer client `includes` | Leave client-side (small catalog) | — |
| `GET /graphs/search?q=` header substring | Keep (headers ≠ payloads) | — |
| `clone()` Composer snapshot | Unchanged (not `copyGraph` / `mergeGraph`) | WI-005 |
| Entity/edge/graph JSON clocks | **C-19** | — |

---

## Domain stays in the example

Do **not** move to core:

- SBOM: application/version/BOM/fingerprint/portfolio tables; Combined SBOM **GET** union (`BomUnion`); MI *report meaning*; CycloneDX; `owner` annotation; app→app depends **inference** after reverse lookup
- AR: `ar_collection` metadata, accepted types, write modes, preprocessing/event SPI, “used in collections” join
- Friendly relation labels (`DEPENDS_ON` → “Depends on”)

---

## Docs per feature WI

Same commit as code (already done for WI-002…006):

1. [`docs/design/graph/apps-vs-foundation.md`](../../../design/graph/apps-vs-foundation.md) — shipped names
2. Product design if the **domain** API changes: [`docs/design/sbom/example.md`](../../../design/sbom/example.md) and/or [`docs/design/asset-repository/example.md`](../../../design/asset-repository/example.md)
3. Operator README if a user-visible route appears (`examples/sbom/README.md`, `examples/asset-repository/README.md`)
4. Core KDoc on the new public API

WI-007 only **sweeps** leftovers; it is not the first write-up of each API.

Cross-app store jobs this story: **catalog helpers (WI-002)**, **paging/counts (WI-006)**. Text `q` is **C-20**.
