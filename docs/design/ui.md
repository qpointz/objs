# Objs UI user manual

The Objs workbench is a browser UI for exploring stored graphs, inspecting registered schemas,
and authoring object-schema DSL definitions.

## Current graph

There is **no global graph** and **no pack chrome**. Graphs are durable `bom_graph` headers with
member entities and graph-local edges (see [`graph/model.md`](graph/model.md)).

| Surface | Behaviour |
|---------|-----------|
| **Explorer** | **Read-only.** Two exclusive modes: **Graph** (one opened graph) or **Selection** (matcher result set). Never mutates the store. |
| **Objects** | **Read-only.** Pool/cross-graph object search; client **shelf**; **New graph from shelf** → Composer (replace draft). |
| **Composer** | Draft workspace; **Save** / **Snapshot**; owns all writes (including first Save that creates a graph from a Selection / shelf handoff). |
| Matchers | **`all`** / **`graph-expr`** / **`obj-expr`** / **chained** only |
| Schema catalog | Unchanged (global, not graph-scoped) |

**Must have:** visible graph / explore-scope context; Open graph search (incremental, ≤15 hits — not a full catalog list); no whole-store Exec/Save. Snapshot hierarchy UI is **not** part of the objs workbench — that is an application concern (e.g. SBOM). Composer **Snapshot** (copy clean graph → new independent graph) is separate from Save.

### Chrome levels (Explorer / Composer / Query)

| Level | Role |
|-------|------|
| **L0** | App view nav (`AppLayout`) — unchanged |
| **L1** | Short title (`Title order={3}`) + help-icon **popover** (or Schema subtitle) \| primary actions **`size="sm"`** on the right \| Explorer Explore-scope / Composer·Query graph strip |
| **L2** | Tabs alone (Visual/Text/…) when present; workspace handoffs / canvas toolbars use **`size="xs"`** (Schema Format/Lint pattern). Schema catalog puts Apply layout / Import on L1 at `sm`. |
| **L3** | Canvas / draft / script / results |

**Size baseline (Schema):** page-header actions `sm`; in-panel / canvas toolbars `xs`; avoid `compact-sm` / `compact-xs` for shared chrome.

## Start and open

Start the workbench runner (`:objs-service-app`, port **8081**), then open:

```text
http://localhost:8081/workbench/
```

For PostgreSQL:

```powershell
docker compose -f deploy/local-dev/docker-compose.yml up -d
./gradlew :objs-service-app:run --args="--spring.profiles.active=postgres"
```

(The SBOM inventory app is a separate process: `./gradlew :sbom-service:run` on port **8080**.)

The **top header** shows **Workbench** (links home), then the view switcher, and a compact
dark/light toggle on the right:

| View | Path | Purpose |
|------|------|---------|
| **Explorer** | `/workbench/explorer` | Read-only explore: Graph mode or Selection mode; hand off to Composer / Query |
| **Objects** | `/workbench/objects` | Pool object search + shelf; **New graph from shelf** → Composer |
| **Composer** | `/workbench/composer` | Draft workspace: Visual/Text edit, Validate / Save / Snapshot |
| **Query** | `/workbench/query` | Tabs Query (script) / Matcher / Options; Exec → traverse API; Structured / Raw results |
| **Schema** | `/workbench/model` | Browse and edit object/edge schemas |

L0 header order: **Explorer · Objects · Composer · Query · Schema**.

## Objects

Objects (`/workbench/objects`) is **read-only** pool exploration:

1. **Search** — shared `MatcherQueryForm` (default `obj-expr`); bare `obj-expr` uses
   `POST /entities/query` (orphans included). Result grid matches Composer **Add objects**.
2. **Shelf** — client cart of entities (unique by id); add/remove from the grid; persisted in
   `localStorage` (`objs.ui.objects.shelf`).
3. **New graph from shelf** — navigates to Composer with `graphId: null`, `replaceDraft: true`,
   and `graphContents: { entities: shelf, edges: [] }`. First **Save** in Composer creates the graph.

Chrome: L1 title + help + primary actions (`sm`); main = matcher + table; right pane = shelf list
(`xs` in-pane actions).

## Graph explorer

Explorer is **read-only**: it may open graphs, run matchers, and display results. It does **not**
call create / mutate / delete graph APIs. All writes happen in **Composer**.

### Modes (either/or)

| Mode | Entered by | Canvas | Clears |
|------|------------|--------|--------|
| **Graph** | **Open graph…** | Members of one `bom_graph` | Selection canvas / matcher result |
| **Selection** | Matcher **Exec** | Matcher hit set (may span graphs) | Opened graph id / Graph-mode header |

Switching mode **resets** the previous mode’s view.

### Explore-scope fragment

Pill switcher **Graph | Selection** (same optical pattern as Open graph Search | Expression).
Each mode shows **only** its active content:

- **Graph:** **Open graph…** + opened-graph readout (id + annotation pills), or empty prompt
- **Selection:** Matcher + **Exec**, then N objects / M edges + matcher one-liner

Open graph / Exec still flip the active mode (and canvas). The switcher can also change mode
directly; either/or canvas rules below still apply.

Title row: short **Graph explorer** + help-icon popover (former subtitle copy; no docs links).

Matchers use shared `MatcherQueryForm` (**`all`** / **`graph-expr`** / **`obj-expr`** / **chained**):

- **`all`** — union of stored members/edges across every graph (distinct by id); orphans excluded.
- **`graph-expr`** — JEXL over graph header `id` and `a.*`; matching graphs contribute stored members/edges.
- **`obj-expr`** — JEXL over entity fields. With an opened graph: scoped to that graph. With **no**
  opened graph: `POST /entities/query` over the **pool** (orphans included; equality/`&&` SQL
  pushdown). `all` / `graph-expr` still use `/graphs/query`.
- **chained** — Visual builder or JSON array of stages.

```json
[
  { "graph-expr": "a.env == 'prod'" },
  { "obj-expr": "type == 'Component' && p.kind == 'library'" }
]
```

1. Configure the matcher (or Open graph…).
2. **Exec** / Open graph loads the canvas (mode switch as above).
3. Select a node or edge to inspect. Schema type links open in a **new browser tab**.
4. **L2:** **Apply layout ▾**; mode exits below.

### L2 handoffs (entire canvas)

| Action | When | Behaviour |
|--------|------|-----------|
| **Open in Composer** | Graph mode | Navigate with `graphId`; Composer loads members from API |
| **New graph from selection** | Selection mode | Navigate with `graphId = null`, draft = **entire** canvas; **always replaces** Composer draft. First **Save** in Composer creates the graph (membership of same entity ids + edge upserts). |
| **Open in Query** | Canvas non-empty | Pass entire canvas (+ matcher context) as traverse input |

Empty canvas: **Open in Query** disabled.

The last successful matcher may be kept in `localStorage` (`objs.ui.graphExplorer.matcher`). Session
restore for the Selection canvas may use `objs.ui.graphExplorer.session`.

After **Exec**, the matcher row shows wall-clock query time plus node/edge counts.

### Selection history

Node and edge selection is stored in the URL (`?qid=<uuid>&node=<id>` or `&edge=<id>`). Each
successful **Exec** mints a new `qid` (also persisted in the session). Browser **Back** / **Forward**
restores selection only when the URL `qid` matches the current result set; otherwise the inspector
clears.

### Inspect a node

Selecting a node shows:

- entity name, type, schema version, and ID;
- annotations used for graph selection;
- JSON payload.

Type / schema links open the schema detail in a **new browser tab**.

### Inspect an edge

Selecting an edge shows:

- role, type, schema version, and ID;
- source and target IDs;
- edge properties.

Schema links open in a **new browser tab**.

When the edge has a property schema, select **Open edge property schema** to inspect it.

## Query

Query runs a **gremlin-lang** script against the subgraph selected by a matcher
(`POST /api/v1/objs/graph/traverse/gremlin`). See [`graph/gremlin.md`](graph/gremlin.md).

1. Open `/workbench/query` (or **Open in Query** from Explorer with a non-empty canvas).
2. Top tabs (**L2** hosts **Exec** + stats beside the tab strip):
   - **Query** — script editor only (Groovy highlighting; wire language remains `gremlin-lang`).
   - **Matcher** — shared `MatcherQueryForm` (same modes as Explorer / Composer).
   - **Options** — eval timeout (`traversalOptions.timeoutSeconds`, default 60).
3. Drag the horizontal splitter to enlarge the top pane when the script is long.
4. **Exec** — runs matcher → materialize → script; shows duration / subgraph stats.
5. Result tabs:
   - **Structured** — tactical view: graph canvas when `subgraph` is present, else table / scalar /
     short fallback. Demo-grade; not a final result UX.
   - **Raw** — pretty-printed full `BoMGremlinResult` JSON.

Script and matcher (and top-pane height) persist in `localStorage` under `objs.ui.query.*`.

## Schemas

Schemas is a single workbench for browsing and editing catalog types.

### Full schema (overview)

Opening **Schemas** without a type selected shows the **Full schema** overview:

- ontology graph of all **ENTITY** object types and allow-list edges (wildcard `*` as one node);
- **Visual** / **Text** tabs: Visual shows the ontology graph; Text is a read-only catalog export with
  a **JSON Schema** / **Seeds** segmented control (same pattern as the type editor JSON/YAML toggle).
  When **JSON Schema** is selected, Text shows export options shared with Export:
  - **Include edges** — `None` / `Outbound` / `Linked` (`includeEdges`);
  - **Edge property schemas** switch (`includeEdgePropertySchemas`; disabled when edges are None);
  - **Dialect** — `2020-12` (only dialect in v1);
  - **Go to type…** — searchable jump to a `$defs` entry (JSON Schema) or ObjectSchema type (Seeds);
  - in-document find via Ctrl/Cmd+F in the code editor;
- click a type node (or a row in the type list) to open that type’s latest version;
- nodes are draggable; positions and layout direction are kept in `localStorage`
  (`objs.ui.fullSchema.layout`) and restored on return (new types still use auto layout until moved);
- **Apply layout** (with direction menu: TB / LR / BT / RL) re-runs automatic layout and clears
  saved node positions;
- **Export** menu downloads either catalog seed YAML
  (`GET /api/v1/objs/registry/export?format=seeds`) or full-catalog JSON Schema
  (`…?format=json-schema` plus the current overview options; menu hint shows e.g. `outbound · 2020-12`);
- **Import** MERGEs a catalog YAML (`POST /api/v1/objs/registry/import?format=seeds`). Files that
  contain `Graph` documents are rejected. Import never deletes catalog entries.
- **Refresh** reloads schemas and edges.

Edge-property schemas appear in the type list (**E**) but are not nodes on the overview graph.

### Type list

- Flat list of all types with an **O** (object / `ENTITY`) or **E** (edge / `EDGE_PROPERTIES`) pill.
- Click a type to open its **latest** version.
- **Create** menu: Create New Object / Create New Edge.

### Type detail

From a type, **Full schema** returns to the overview. The detail toolbar includes:

- Version selector, **Create version** (dialog: base version + new version), and **Delete**
  split button (**Delete version** / **Delete schema**).
- **Save** for the opened version.
- New drafts use **Create schema**.
- **Lint** lives on the Schema tab (Editor, YAML, and JSON sub-views).

### Editors

Tab order: **Visual**, **Schema**, **Edges** (objects).

- **Visual** — read-only relationship graph of allow-list neighbours (ego view for the selected type).
- **Schema** — consolidated content editor with sub-views:
  - **Editor** — recursive content-schema tree editor
  - **YAML** / **JSON** — full schema document text editor; **Format**, **Rollback**, **Lint**, and
    **New UUID** (toast auto-hides after 3s)
  - **JSON Schema** — generated projection (read-only; existing schemas only)
- **Edges** — allowed inbound/outbound edge rules for object schemas (add, edit, delete).

Unsaved edits show an **Unsaved changes** badge with **Rollback** to the last loaded/saved
snapshot. Switching type, version, create draft, or leaving Schemas opens a confirmation dialog
(Stay / Leave). Browser close/reload is also blocked while dirty.

### Edges (objects)

Object schemas include an **Edges** tab with the allowed-edges table (inbound then outbound).
You can **add**, **edit**, and **delete** rules (direction, related type, role, cardinality,
properties NONE or SCHEMA). Edge edits stay local until **Save** (with content-schema
edits); **Rollback** restores both. Editing identity fields (source / role / target) replaces the
draft rule. Edge-property schemas edit payload DSL only — relations are authored on the object
Edges tab, not on the edge schema.

## Schema explorer (legacy name)

The former Schema explorer / Schema linter split is replaced by **Schemas** above.

Schema explorer lists all persisted schema definitions. Use the tabs above the list to show:

- **Entity** — schemas used for entity payloads;
- **Edge props** — schemas used for edge properties;
- **All** — entity and edge-property schemas.

Use the search field to filter by type or version. Each schema type can have multiple versions;
select a version badge to open that exact definition.

### Schema details

An entity schema displays:

- its type and version (kind is shown in the type list as O/E);
- content editors (**Visual**, **Schema**, **Edges**);
- generated JSON Schema 2020-12 under Schema → **JSON Schema**.

The **Edges** tab lists allowed inbound then outbound rules, with direction icons
(`IconArrowNarrowLeftDashed` / `IconArrowNarrowRightDashed`).

Source and target types in allowed-edge tables link to their schema definitions. Wildcard `*`
means the rule applies to every entity type in that position.

An edge-property schema also displays every allowed source–role–target relation that uses it.
Its visual graph places source entities on the left, the edge schema in the center, and target
entities on the right.

### Visual schema graph

Open the **Visual** tab for a condensed UML-like relationship view:

- the selected entity is the detailed center block;
- its fields are listed as `name : TYPE`, with `*` marking required fields;
- nested object fields are indented;
- arrays show their item type, for example `ARRAY<STRING>`;
- incoming entity types appear on the left;
- outgoing entity types appear on the right;
- arrows show direction and are labelled with the allowed-edge role and cardinality
  (`ROLE · 1:1` / `ROLE · 1:*`; role only when `UNSPECIFIED`).

Related entities are intentionally shown as compact name-only blocks. Select one to navigate to
that entity's latest registered schema, then continue traversing its relationships. `Any type (*)`
represents a wildcard rule and is not navigable.

### Edit or create a version

- **Edit / lint** opens the selected version in Schema linter. **Save** replaces that same
  version.
- **Create version** opens the selected definition as the starting point for a new major version.

Existing versions are not implicitly changed when creating a version.

## Schema linter

Schema authoring is under Schemas → **Schema**, with synchronized sub-views:

- **Editor** — recursive schema tree and field/node editor;
- **YAML** / **JSON** — direct editing of the complete schema document;
- **JSON Schema** — read-only generated projection.

The schema header contains:

| Field | Meaning |
|-------|---------|
| **Type** | Stable schema type name |
| **Opened version** | Exact version being edited |
| **Usages** | `ENTITY`, `EDGE_PROPERTIES`, or both |

Type and version are editable for a new draft and fixed when editing an existing schema.

### Edge-property schema editor

When **Usages** contains `EDGE_PROPERTIES`, the linter displays an **Allowed entity relations**
editor above the object-schema editor. An edge-property schema has two parts:

1. the object definition used to validate edge properties;
2. one or more directed relations that are allowed to use those properties.

For each relation select:

| Field | Meaning |
|-------|---------|
| **Source entity** | Allowed source type, or `Any type (*)` |
| **Role** | Directed relationship role such as `DEPENDS_ON` |
| **Target entity** | Allowed target type, or `Any type (*)` |
| **Cardinality** | `UNSPECIFIED`, `1:1` (singular), or `1:*` (many); default `UNSPECIFIED` |
| **Empty properties allowed** | Whether an edge may omit the property object |

Use **Add relation** to associate another source–role–target triple with the same property schema.
Use **×** to remove a relation. Source and target selectors list registered entity schemas.

The linter rejects blank and duplicate relation triples. Saving persists the property definition
and replaces the relation list associated with that exact edge-schema version.

### Visual mode

Select a node or field in **Content schema** to edit it.

Supported node types:

- `OBJECT`
- `ARRAY`
- `STRING`
- `NUMBER`
- `INTEGER`
- `BOOLEAN`
- `ENUM`

For object fields, use:

- **+** or **Add field** to add a field;
- **↑** and **↓** to change field order;
- **×** to remove a field;
- **Required** to control whether the field is mandatory.

Arrays expose their item schema recursively. Strings support known formats such as `uri`,
`date-time`, and `email`. Enums contain ordered values and descriptions.

Changing a node's type replaces its type-specific configuration with a valid starter definition.
For example, changing a node to `ARRAY` creates a string item definition that can then be edited.

### YAML / JSON mode

YAML and JSON sub-views expose the complete authoring document:

- **Format** reformats the current document.
- **Rollback** restores the last loaded or saved schema snapshot (also available next to
  **Unsaved changes**).
- Switching back to **Editor** (or leaving the Schema tab) parses the current document. Invalid
  source stays in the text view and shows an error.

An entity schema document has this shape:

```yaml
type: Component
version: 1.0.0
usage: ENTITY
contentSchema:
  type: OBJECT
  title: Component
  description: Component payload
  fields: []
```

For an edge-property schema, use `usage: EDGE_PROPERTIES` and the same `contentSchema` shape.
Allowed edge rules are managed on the object **Edges** tab (and registry edge endpoints), not in
the YAML/JSON document.

For a new draft, type, version, usage, and content definition can all be edited directly.
When editing an existing schema, the text document requires type and version to remain equal to
the opened catalog entry; use **Create version** for a new version.

### Lint

Select **Lint** to validate without saving. The server:

1. parses the DSL into the typed schema model;
2. validates and normalizes the complete recursive definition;
3. generates a JSON Schema preview.

A successful result shows **valid**, the normalized definition, and generated JSON Schema. An
invalid result shows an issue code and message. Lint never persists changes.

### Save

When an existing version is opened, **Save** validates and replaces that exact
`(type, version)` catalog entry.

Use this only when correcting or intentionally revising the published version. Existing entities
that reference the version will be validated against the updated definition on subsequent writes.

### Create schema

From the standalone Schema linter:

1. enter a new type and initial version;
2. choose usage (`ENTITY` or `EDGE_PROPERTIES`);
3. author and lint the definition;
4. select **Create schema**.

The initial version is created exactly as entered.

### Create version

From an existing schema, **Create version** opens a dialog:

- **Base version** — any existing version for the type (default: currently selected). Seeds the
  draft’s initial content.
- **New version** — the version string to create (default: next major after the latest registered
  version for that type, e.g. `4` → `5`, `4.2.1` → `5.0.0`).

Confirm enters a local **draft** (**Unsaved changes**). Nothing is persisted until **Save**,
which calls `PUT .../schemas/{type}/{newVersion}` with the authored body. Existing versions are
never overwritten (`409` if the version already exists). **Rollback** discards the draft and
returns to the base version.

### Delete

On an existing schema detail:

- **Delete version** — removes the opened version (`DELETE .../schemas/{type}/{version}`). Confirm
  by typing the version string. Navigates to another remaining version, or Full schema if none.
- **Delete schema** — removes all versions of the type and allow-list edges where the type is
  source or target (`DELETE .../schemas/{type}`). Confirm by typing the type name. Returns to Full
  schema.

## Composer

Composer (route `/workbench/composer`; title **Composer**, not “Object linter”) is the **draft
workspace** for editing graph membership and payloads. There is **no Browse schemas** on this page
(use L0 Schema). Selected object/edge schema links open in a **new browser tab**.

| Action | Behaviour |
|--------|-----------|
| **Open graph…** | Shared search dialog (`GET …/graphs/search`); loads members; sets current graph |
| **New graph** | Clears draft and clears graph id (empty edit session) — does **not** create a server graph |
| **Add objects…** | Visual canvas toolbar; side pane search (same matcher as Explorer) |
| **Validate** | Dry-run mutation |
| **Save** | Enabled when dirty or `graphId == null` (or never-saved). With **no** graph id: **creates** graph (`entityIds` membership + edge upserts). With id: `PUT …/graphs/{id}` mutation |
| **Snapshot** | Enabled only when saved + clean; clone dialog → new independent graph; switches to new id |

L1: title + help popover; **Reset** / **Clear** / **Validate** / **Save** / **Snapshot** (`size="sm"`, same row).  
Tabs: Visual / Text only. Visual L2 toolbar: **New** ▾ / **Link** / **Add objects…** + draft actions + **N on canvas** / last-search badges (`size="xs"`).

Empty selection: side pane may edit **graph-level annotations**.

Edit form: no duplicate Payload/Annotations section titles; per-field **delete** omits payload keys (shows **deleted**); **Schema ▾** migrates to another **version of the same type** only (key→key; confirm on zero/partial).

| API | |
|-----|--|
| Add objects / Search | With current graph: `POST /api/v1/objs/graphs/{id}/query`. With **no** graph + `obj-expr`: `POST /api/v1/objs/entities/query` (pool, orphans included). `all` / `graph-expr`: `POST /api/v1/objs/graphs/query` |
| Validate | `BoMGraphMutation` dry-run |
| Save (existing graph) | `PUT /api/v1/objs/graphs/{id}` |
| Save (no graph id) | `POST /api/v1/objs/graphs` with `entityIds` + edge upserts |
| Snapshot | Clone semantics (`POST …/graphs/{id}/clone` or equivalent) |
| Open graph… | `GET /api/v1/objs/graphs/search` then `GET …/graphs/{id}` |

Returning to Composer / Explorer with a persisted current graph id **reloads** that graph’s members into the canvas (not annotations only), so chrome and Visual stay consistent. If the graph is missing, the current-graph selection is cleared.

**New UUID** (clipboard + toast) sits on the Text tab toolbar next to Format / Rollback.

### Tabs

| Tab | Role |
|-----|------|
| **Visual** | React Flow canvas with resizable right side pane (edit form or Add objects); canvas toolbar create/draft actions + layout |
| **Text** | YAML/JSON of the **mutation only** (`upsert` + `delete`). Unchanged baseline objects stay on Visual but are omitted from Text until edited, created, or deleted. |

Invalid Text blocks switching to Visual; the last good draft is preserved.

### Add objects / exclude vs delete

1. Open **Add objects…** (Visual canvas toolbar) — search UI in the **right side pane**. Defaults matcher mode to **`obj-expr`**.
2. Drag the vertical splitter between canvas and side pane (width persisted).
3. **Search** fills a results table; paginate locally at **20** rows. Successful Search mints a new **`qid`**.
4. **Add** / **In draft** toggles merge or exclude; merges induced edges among store-backed draft ids.
5. **Done** refreshes induced edges and closes the panel.
6. Explorer **Open in Composer** (Graph) loads that graph by id. Explorer **New graph from selection** **replaces** the draft with the entire canvas and clears graph id.
7. **Remove from draft** excludes without pending-delete chrome. **Delete** soft-deletes baseline ids for Apply.
8. Draft status icons: **+** new, **pencil** modified, **−** deleted.
9. **Reset** restores the last rollback snapshot; **Clear** empties the draft.

**New linked** always copies annotations from the selected source object (including when empty). **New** seeds from graph-header annotations.

### Validate and Save

1. Open Composer.
2. Optionally **Add objects…**, Open graph, or receive an Explorer handoff.
3. **Validate** or **Save**. Use **Snapshot** only on a clean saved graph.

Save clears pending deletes and refreshes the baseline on success.

```yaml
upsert:
  entities: []   # creates / updates only
  edges: []
delete:
  entities: []   # loaded entity ids
  edges: []      # loaded edge ids
```

Loaded baseline objects appear in Text only after they are modified, newly created, or listed under `delete`. Validate/Save send this same `BoMGraphMutation` envelope.

Entities that are referenced by mutation edges need explicit UUIDs. Edge source/target types, role,
property-schema reference, and properties are validated against the current registry.

### Open graph search (all views)

Shared dialog on Explorer / Composer / Query:

```http
GET /api/v1/objs/graphs/search?q={text}&limit=15&expr={graph-expr}
```

Empty `q` without `expr` → empty list (never dump all graphs). Response `{ "items": [ { "id", "annotations" } ] }`.
v1 match: id / UUID-prefix + case-insensitive substring on id and annotation strings. Extensible later (additive fields/params; FTS deferred).

Hit list uses **compact fixed-height** graph rows (single-line id + pills; list scrolls). Bars
(Composer / Explorer / Query) keep the comfortable multi-line `GraphHeaderReadout`.

## Common errors

| Message or condition | Resolution |
|----------------------|------------|
| Graph query returns no nodes | Verify `graph-expr` / `obj-expr` criteria exist on stored graphs/entities and that chained stages retain results |
| Matcher query is rejected | Correct the matcher shape: one `graph-expr`/`obj-expr` object or a non-empty JSON array of matcher objects; without a current graph, bare `obj-expr` is auto-wrapped with `all` |
| Schema cannot be loaded | Confirm the selected type and version still exist |
| Source document is invalid | Correct YAML/JSON syntax before switching to Schema mode or saving |
| `SCHEMA_DEFINITION_INVALID` | Correct the field named in the lint message |
| Title, description, or field name is blank | Supply a nonblank value |
| Duplicate object field or enum value | Rename or remove the duplicate |
| Unsupported string format | Select one of the formats offered by the visual editor |
| Database validation fails after a schema change | Recheck compatibility between the updated schema version and stored payloads |

## Development mode

To run the UI separately with live reload:

```powershell
Set-Location objs-service-ui
npm install
npm run dev
```

Open `http://localhost:5173/workbench/`. Vite proxies `/api` requests to
`http://localhost:8080`, so `objs-service-app` must also be running.

Packaged builds: Gradle `:objs-service-ui` (node-gradle) builds the SPA into
`classpath:/static/workbench/` on the module JAR; `:objs-service-app` depends on it
(`runtimeOnly`). Skip with `-PskipUi=true`. Served at `/workbench/` and does
**not** require `:objs-sbom-example`. `WorkbenchSpaRoutingFilter` forwards
client routes (including schema versions like `1.0.0`) to `/workbench/index.html`
so a browser refresh is not a 404.

