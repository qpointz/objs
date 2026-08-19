# Workbench as a foundation consumer (C-17)

`:objs-service` + `:objs-service-ui` are the **workbench**, not a product app. They already sit on `:objs-core`. Anything the workbench reimplements that SBOM/AR also reimplement belongs in core — then **workbench + both examples** call it.

## What the workbench does today

| Surface | Mechanism | Remaining gap |
|---------|-----------|----------------|
| `POST /entities/query` | `selectFromPool(obj-expr)` — equality/`&&`/`\|\|` SQL pushdown; optional `page`/`size` (WI-006) | No substring/`q` (**C-20**) |
| `GET /entities` | `listEntities()` full pool | Unpaged listing; query path is paged |
| `GET /graphs/search?q=` | Header substring on id + annotation keys/values (**not** FTS) | Entity payload `q` does not exist (**C-20**) |
| `GET /graphs/{id}/query` | Graph-scoped matcher | Same operator limits |
| `GET …/types/{type}/edges` | Core `allowedEdgesForType` (WI-002) | — |
| `POST /graphs/{id}/clone` | Hard `clone()` (new entity ids) | Distinct from membership `copyGraph` / `mergeGraph` (WI-005) |
| SPA filter | `SpaRoutingFilter` in `:objs-service` | SBOM copies the class to avoid compile-dep (G-X4, not this story) |

## What the workbench UI does in the browser

| Surface | Mechanism | Remaining gap |
|---------|-----------|----------------|
| Objects page | Matcher form → `POST /entities/query?page=1&size=100` | Paging shipped (WI-006); optional `q` is **C-20** |
| Schema explorer type list | `type/version.toLowerCase().includes(q)` | Catalog list is small; **not** entity FTS |
| Schema portals in **examples** | Same client `includes` | Same — not pool search |
| Open-graph modal | `/graphs/search?q=` | Already store-side substring on **headers** |

## In this story (workbench called core)

| WI | Workbench change |
|----|------------------|
| WI-002 | `edgesForType` → core `allowedEdgesForType`; object rows may use `displayLabel` |
| WI-006 | `POST /entities/query?page=&size=` pages pool results; Objects search uses page=1&size=100 |

WI-003 / WI-004 / WI-005 are optional for workbench UI this story (no “where used” / duplicates / membership-copy / merge screens). Core `copyGraph` / `mergeGraph` still exist for later workbench wiring.

## Not foundation / not this story

- Matcher **form** chrome (visual chain builder) — workbench-only UI
- Gremlin Query page — already `:objs-gremlin-core`
- Full-catalog JSON Schema export — already core
- Linguistic Postgres FTS (`tsvector` / ranking) — later
- Payload text `q` / contains — **C-20** [`store-text-search`](../../planned/store-text-search/STORY.md)
- Domain `LIKE` on **product** tables (SBOM application name, AR collection name) — stays in the example
- Extracting `SpaRoutingFilter` (G-X4)
