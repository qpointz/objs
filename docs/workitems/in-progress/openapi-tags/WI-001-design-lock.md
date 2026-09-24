# WI-001 — Design lock: tags, completeness, codegen

**Status:** done  
**Examples:** docs  
**Depends on:** WI-000

## Goal

Lock the **target matrix** and confirm GAPS G-0…G-10 (all closed in [`GAPS.md`](GAPS.md)).

## Normative matrix

### Springdoc groups (OpenAPI definitions)

| Group id | Module / owner | `pathsToMatch` (normative) |
|----------|----------------|----------------------------|
| **`graph`** | `:objs-service` (+ gremlin/jgrapht on classpath) | `/api/v1/objs/graph`, `/api/v1/objs/graph/**`, `/api/v1/objs/graphs`, `/api/v1/objs/graphs/**`, `/api/v1/objs/entities`, `/api/v1/objs/entities/**`, `/api/v1/objs/edges`, `/api/v1/objs/edges/**`, `/api/v1/objs/status` |
| **`registry`** | `:objs-service` | `/api/v1/objs/registry/**` |
| **`policy`** | `:objs-policy-service` (bean preferred here) | `/api/v1/objs/policy`, `/api/v1/objs/policy/**` |
| **`inventory`** | `:sbom-service` | `/api/v1/inventory`, `/api/v1/inventory/**` (unchanged) |
| **`asset-repository`** | `:asset-repository-service` | `/api/v1/asset-repository`, `/api/v1/asset-repository/**` (unchanged) |

No separate groups for `edges`, `traverse`, or `graph-algorithms`.

### Objs topic tags (`/api/v1/objs/**`)

| Tag | Controllers / ops |
|-----|-------------------|
| **`graphs`** | Named-graph header: list, search, create, get, update annotations, delete |
| **`mutations`** | Graph mutate merge/replace + validate |
| **`entities`** | Pool entity CRUD (`ObjsEntitiesController`) **and** graph attach/detach |
| **`edges`** | Pool edge history (`ObjsEdgesController`) **and** graph-scoped edge CRUD |
| **`query`** | In-graph / cross-graph / version query |
| **`versions`** | Graph version create/list/get/purge/reset/apply-structure |
| **`housekeeping`** | clear, destroy, clone, purge-all-versions |
| **`seeds`** | `ObjsGraphController` seed validate/import/export (was tag `graph` / briefly `graph-seeds`) |
| **`status`** | `ObjsStatusController` |
| **`traverse`** | Gremlin traverse |
| **`algorithms`** | JGraphT capabilities / cycles (was `graph-algorithms`) |
| **`catalog`** (registry group) | refresh, import, export |
| **`schemas`** (registry group) | schema type@version CRUD, lint, JSON Schema |
| **`edges`** (registry group) | relation allow-list rule list/upsert/delete |
| **`catalog`** (policy group) | capabilities, export, categories, policies CRUD |
| **`evaluate`** (policy group) | check, evaluate |
| **`suites`** (policy group) | suites CRUD, selection, suite evaluate/persist |
| **`archives`** (policy group) | evaluation list/get/delete |

Tag names omit group prefixes (`policy-…`, `graph-…`) so they stay short inside each springdoc group.

Method-level `@Tag` on `ObjsGraphsController` / `ObjsPolicyController` where one class spans topics.

### SBOM topic tags

| Tag | Ops |
|-----|-----|
| **`applications`** | App search/CRUD/stats/depends-on |
| **`application-versions`** | Version CRUD, combined, dependents, promote, export, fingerprints |
| **`application-sboms`** | BOM CRUD / save / assets / relations on a version |
| **`assets`** | `AssetInventoryController` (keep) |
| **`portfolios`** | `PortfolioController` (keep) |
| **`schemas`** | `InventorySchemaController` (keep) |
| **`assessment`** | `AssessmentController` (keep) |

Group remains **`inventory`**.

### AR topic tags

| Tag | Ops |
|-----|-----|
| **`collections`** | collections CRUD/copy/statistics |
| **`objects`** | objects, search, compositions, relations |
| **`schemas`** | schema-catalog / schemas browse (AR domain) |

Group remains **`asset-repository`**.

### Completeness bar (G-7)

Every public op: summary (+ description when non-obvious); parameter descriptions; DTO + field `@Schema` descriptions; HTTP status codes with response schemas for success and important errors. Objs is the main gap; re-audit SBOM/AR.

### Codegen harness (G-8…G-10)

| Item | Decision |
|------|----------|
| Tool | `openapi-generator-cli` (pin in script/docs) |
| Languages | Java + Python |
| Primary URLs | `{base}/v3/api-docs/graph`, `…/registry`, `…/policy` |
| Default base | `http://localhost:8080` (override for workbench **8081**) |
| Output | temp/gitignored — **never** commit generated sources |
| Bar | Java compiles; Python `compileall` (or import) |
| SBOM/AR | readiness check only (optional light harness mode) |

## Acceptance

- [x] G-1…G-10 closed (see GAPS)
- [x] Target matrix above
- [x] No production code in this WI
