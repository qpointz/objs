# Objs UI user manual

The Objs workbench is a browser UI for exploring stored graphs, inspecting registered schemas,
and authoring object-schema DSL definitions.

## Shared graph context

There is **no global graph** and **no pack chrome**. Graphs are durable `objs_graph` headers with
member entities and graph-local edges (see [`graph/model.md`](graph/model.md)).

**Explorer · Objects · Query** share one **graph context** (`GraphContextProvider`). Changing context
in any of those three updates all three. **Composer** and **Schema** do **not** bind to it.

| Context kind | Meaning |
|--------------|---------|
| **Graph** | One opened graph id (+ optional version pin) |
| **Matcher** | Matcher expression materialized to entities/edges |
| **All** | `{ all: true }` — union across every graph |

| Surface | Behaviour |
|---------|-----------|
| **Explorer** | Read-only canvas + object viewer for the shared context |
| **Objects** | Entity list from context; shelf + Matcher side pane; **New graph from shelf** → Composer |
| **Query** | Gremlin script against context subgraph; Visual / Data / Raw results |
| **Composer** | Draft workspace (separate graph id); **Save** / **Create version** / **Clone** |
| **Schema** | Global catalog; not graph-scoped |

**Node cap (~300):** Explorer and Query **Visual** disable the graph canvas when the context or
result exceeds **300** nodes. Use Data / Raw or narrow the context.

Composer **Create version** freezes the **same** graph id. **Clone** is a new-id deep copy.
Explorer no longer has a left **Versions** pane — version pin lives on the shared context bar.

### Chrome layout (all product views)

| Row | Explorer / Objects / Query | Composer / Schema |
|-----|---------------------------|-------------------|
| **1** | `Title order={3}` + shared or local context bar (Paper) | Same pattern |
| **2** | View actions at **`sm`** (`VIEW_ACTION_BUTTON_SIZE`) | Same |
| **3** | Workspace (canvas, grid, script, catalog, …) | Same |

In-tab / canvas toolbars use **`xs`** (Schema Format/Lint, Query script area, Composer Visual toolbar).

| Level | Role |
|-------|------|
| **L0** | App view nav (`AppLayout`) |
| **L1–L3** | As above |

**Size baseline:** view-level actions `sm`; in-panel / canvas toolbars `xs`.

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

(The SBOM inventory app is a separate process: `./gradlew :sbom-service:run` on port **8080**.
Inventory UI: [`sbom/user.md`](sbom/user.md).)

The **top header** shows **Workbench** (links home), then the view switcher, and a compact
dark/light toggle on the right:

| View | Path | Purpose |
|------|------|---------|
| **Explorer** | `/workbench/explorer` | Read-only explore: Graph mode or Selection mode; hand off to Composer / Query |
| **Objects** | `/workbench/objects` | Pool object search + shelf; **New graph from shelf** → Composer |
| **Composer** | `/workbench/composer` | Draft workspace: Visual/Text edit, Validate / Save / Create version / Clone |
| **Query** | `/workbench/query` | Gremlin script + Visual / Data / Raw results (shared context) |
| **Schema** | `/workbench/model` | Browse and edit object/edge schemas |

L0 header order: **Explorer · Objects · Query · Composer · Schema**.

A **product tour** starts on first visit (`localStorage`: `objs.ui.workbench.tour.v2`). Replay from
the header help icon. Steps follow nav order: shared graph context → each view’s chrome → Schema.
Targets missing on the current page (e.g. graph version pin in matcher mode, schema version on
catalog overview) are **skipped** automatically.

## Objects

Objects (`/workbench/objects`) lists entities from the **shared graph context** (read-only).

**Layout**

1. Row 1 — **Objects** title + `GraphContextBar`
2. Row 2 — exec stats (left); **Add selected to shelf**, **Remove selected from shelf**, **Clear
   shelf**, **New graph from shelf** (right, `sm`)
3. Row 3 — results grid (Query Data-style chrome, page size **25**, virtualize **>200** rows) +
   vertical splitter + right pane

**Right pane**

- **Object inspect** when an Id link is clicked (same sectioned viewer as Explorer)
- Otherwise **Shelf | Matcher** tabs — Search stays in Matcher only; shelf shows type, name, truncated
  id, remove icon

**Shelf** — client cart (`localStorage`: `objs.ui.objects.shelf`). **New graph from shelf** → Composer
with `replaceDraft: true` and shelf entities.

Side pane width: `objs.ui.objects.sidePaneWidth` (splitter, max 50% of host).

## Graph explorer

Explorer is **read-only** for the shared graph context.

**Layout**

1. Row 1 — **Explorer** title + `GraphContextBar` (Open ▾ Graph | Matcher | All; version pin in graph
   mode)
2. Row 2 — type **pills** (click to dim non-matching nodes; × clears); **Analyze cycles** when the
   optional algorithm service is present (violet highlight on cycle regions; × clears); **Open in
   Composer** (graph context) or **New graph from selection** (matcher/all); **Apply layout ▾**
3. Row 3 — canvas (disabled above ~300 nodes) + splitter + **object inspect** pane

**Type pills** — filter highlight on canvas; non-selected types render dimmed (pills stay full
opacity).

**Inspect** — node, edge, or empty canvas (graph header in graph mode). Sectioned **Object viewer**
(Node / Payload / Annotations / Versions). Schema links open in a **new tab**. Node color from
`attributes.color` on the type schema.

**Handoffs**

| Action | When | Behaviour |
|--------|------|-----------|
| **Open in Composer** | Graph context | Navigate with `graphId`; Composer loads members |
| **New graph from selection** | Matcher / All | Replace Composer draft with context entities |

Matchers (`all` / `graph-expr` / `obj-expr` / chained) are entered via **Open ▾ Matcher**, not a
dedicated Explorer matcher row.

Inspect pane width: `objs.ui.explorer.sidePaneWidth`.

## Query

Query runs a **gremlin-lang** script against the **shared graph context**
(`POST /api/v1/objs/graph/traverse/gremlin`). See [`graph/gremlin.md`](graph/gremlin.md).

**Layout**

1. Row 1 — **Query** title + `GraphContextBar`
2. Row 2 — last **Exec** stats (left); **Open in Composer**, **Exec**, **Options** cog (right)
3. Row 3 — script editor (Ctrl/Cmd+Enter) + horizontal splitter + result tabs

**Results** — **Visual** (graph canvas + in-tab object viewer; disabled above ~300 nodes), **Data**
(Structured vertices/edges grids, page **25**), **Raw** (full JSON).

**Options** popover — eval timeout only (`traversalOptions.timeoutSeconds`). No Matcher tab and no
right Options pane (removed in Note 6).

**Open in Composer** — when the last result includes graph contents under the node cap, seeds a new
draft via `replaceDraft`.

Script height: `objs.ui.query.topPaneHeight`. Default script: `objs.ui.query.script`.

## Schemas

Schemas is a single workbench for browsing and editing catalog types. Page chrome matches other views
(Note 9): **Schema** title + context bar, then view actions at **`sm`**, then the type list and main
workspace. Schema does **not** bind to shared graph context.

### Full schema (overview)

Opening **Schemas** without a type selected shows the catalog overview in the main pane:

- context bar: **Schema catalog** with type / edge-rule counts;
- view actions: **Apply layout** (with direction menu: TB / LR / BT / RL), **Export** (YAML seeding
  format, JSON Schema, or JSON Schema codegen), **Import**, and **Create ▾**;

- ontology graph of all **ENTITY** object types and allow-list edges (wildcard `*` as one node);
- **Visual** / **Text** tabs: Visual shows the ontology graph; Text is a read-only catalog export with
  a format **dropdown** (**JSON Schema** / **JSON Schema (codegen)** / **YAML (Seeding format)**).
  When a JSON Schema format is selected, Text shows export options shared with Export:
  - **Include edges** — `None` / `Outbound` / `Linked` (`includeEdges`);
  - **Edge property schemas** switch (`includeEdgePropertySchemas`; disabled when edges are None);
  - **Dialect** — `2020-12` or `draft-07`;
  - **Go to type…** — searchable jump to a `$defs` entry (JSON Schema formats) or ObjectSchema type
    (YAML seeding format);
  - in-document find via Ctrl/Cmd+F in the code editor;
- click a type node (or a row in the type list) to open that type’s latest version;
- nodes are draggable; positions and layout direction are kept in `localStorage`
  (`objs.ui.fullSchema.layout`) and restored on return (new types still use auto layout until moved);
- **Apply layout** (with direction menu: TB / LR / BT / RL) re-runs automatic layout and clears
  saved node positions;
- **Export** menu downloads catalog seed YAML (`GET …/export?format=seeds`), full-catalog JSON Schema
  (`…?format=json-schema`), or POJO-ready JSON Schema (`…?format=json-schema-codegen`) using the
  current overview options (menu hint shows e.g. `outbound · 2020-12`);
- **Import** MERGEs a catalog YAML (`POST /api/v1/objs/registry/import?format=seeds`). Files that
  contain `Graph` documents are rejected. Import never deletes catalog entries.

Edge-property schemas appear in the type list (**E**) but are not nodes on the overview graph.

### Type list

- Left sidebar: **S Schema** returns to the catalog overview; search + flat list of all types with
  an **O** (object / `ENTITY`) or **E** (edge / `EDGE_PROPERTIES`) pill. Drag the splitter between
  list and main pane (`objs.ui.schema.sidePaneWidth`).
- Click a type to open its **latest** version.

### Type detail

Context bar shows type name, **Version:** dropdown (same chrome as graph-context version pin),
kind pill, and catalog tags/attributes. View actions include **Create version**, **Save**,
**Delete ▾**, and **Create ▾**.

- **Lint** lives on the Schema tab (Editor, YAML, and JSON sub-views).
- New drafts: type / version / usage fields on the **General** tab; **Create schema** on the actions row.

### Editors

Tab order: **Visual**, **General**, **Schema**, **Edges** (objects).

- **Visual** — read-only relationship graph of allow-list neighbours (ego view for the selected type).
- **Schema** — consolidated content editor with sub-views:
  - **Editor** — recursive content-schema tree editor
  - **YAML** / **JSON** — full schema document text editor; **Format**, **Rollback**, **Lint**, and
    **New UUID** (toast auto-hides after 3s)
  - **JSON Schema** — generated projection (read-only; existing schemas only)
- **General** — description, then object-schema tags and string attributes. Envelope `color`
  (`#rrggbb` or `nocolor`) is the graph node accent for that type; `nocolor` is theme gray.
  Visual / General / Schema / Edges panels fill the remaining editor height.
- **Edges** — allowed inbound/outbound edge rules for object schemas (add, edit, delete).

Unsaved edits show an **Unsaved changes** badge with **Rollback** to the last loaded/saved
snapshot. Switching type, version, create draft, or leaving Schemas opens a confirmation dialog
(Stay / Leave). Browser close/reload is also blocked while dirty.

### Edges (objects)

Object schemas include an **Edges** tab with the allowed-edges table (inbound then outbound).
Selecting a row highlights **one** list membership (incoming or outgoing), including self-edges
such as `Component DEPENDS_ON Component`. A paper **details** panel below the table shows the same
fields as the edit form (direction, types, role, cardinality, properties, description, verbs, tags,
attributes). **Edit** keeps that layout and swaps values for inputs. **Delete** / **Edit** live on
the details panel, not in the table. Deleted rules stay visible as **Deleted** until **Save** or
**Rollback** / **Restore**. **Add allowed edge** opens the same paper as a blank form.
Edge edits stay local until **Save** (with content-schema edits); **Rollback** restores both.
Editing identity fields (source / role / target) replaces the draft rule. Edge-property schemas
edit payload DSL only — relations are authored on the object Edges tab, not on the edge schema.

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
- content editors (**Visual**, **General**, **Schema**, **Edges**);
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

Composer (`/workbench/composer`) is the **draft write surface**. It does **not** use shared graph
context — `ComposerGraphBar` matches the visual chrome only.

**Layout**

1. Row 1 — **Composer** title + `ComposerGraphBar` (**New ▾** Blank | Matcher, **Open**, stats)
2. Row 2 — **Reset**, **Clear**, **Validate**, **Save**, **Create version**, **Clone** (`sm`)
3. Row 3 — Visual / Text tabs + resizable side pane (`objs.ui.composer.sidePaneWidth`)

| Action | Behaviour |
|--------|-----------|
| **New ▾ Blank** | Empty draft, clear graph id |
| **New ▾ Matcher** | Modal → new draft merged from matcher hits |
| **Open** | Existing graph → draft only (does not change shared context) |
| **Save** | Create or mutate graph |
| **Create version** | Freeze same graph id (saved + clean draft) |
| **Clone** | Deep copy to new graph id |

Schema links from the edit form open in a **new tab**. **Add objects…** uses the side-pane matcher
(same modes as elsewhere).

| API | |
|-----|--|
| Add objects / Search | Always pool / cross-graph (not limited to current graph): `obj-expr` → `POST /api/v1/objs/entities/query` (pool, orphans included); `all` / `graph-expr` → `POST /api/v1/objs/graphs/query`. Objects page in graph context still uses `POST …/graphs/{id}/query`. |
| Validate | `BoMGraphMutation` dry-run — `PATCH …/graphs/{id}/validate` when graph known (MERGE); pool `POST /graph/validate` otherwise |
| Save / Merge (existing graph) | `PATCH /api/v1/objs/graphs/{id}` (MERGE: set + unset) |
| Overwrite… (existing graph) | Confirm, then `PUT /api/v1/objs/graphs/{id}` (REPLACE: set-only full draft; clears unset) |
| Save (no graph id) | `POST /api/v1/objs/graphs` with `entityIds` + MERGE mutate |
| Create version | Freeze (`POST …/graphs/{id}/versions`) |
| Clone | Deep copy (`POST …/graphs/{id}/clone`) |
| Open graph… | `GET /api/v1/objs/graphs/search` then `GET …/graphs/{id}` |

Returning to Composer / Explorer with a persisted current graph id **reloads** that graph’s members into the canvas (not annotations only), so chrome and Visual stay consistent. If the graph is missing, the current-graph selection is cleared.

**New UUID** (clipboard + toast) sits on the Text tab toolbar next to Format / Rollback.

### Tabs

| Tab | Role |
|-----|------|
| **Visual** | React Flow canvas with resizable right side pane (edit form or Add objects); canvas toolbar create/draft actions + layout |
| **Text** | YAML/JSON of the **mutation only** (kind-first `entities`/`edges` × `set`/`unset`). Unchanged baseline objects stay on Visual but are omitted from Text until edited, created, or deleted. |

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
3. **Validate** or **Save**. Use **Create version** only on a clean saved graph.

Save clears pending deletes and refreshes the baseline on success.

```yaml
entities:
  set: []     # creates / updates only (MERGE) or full desired members (REPLACE)
  unset: []   # loaded entity ids (MERGE only)
edges:
  set: []
  unset: []   # loaded edge ids (MERGE only)
```

Loaded baseline objects appear in Text only after they are modified, newly created, or listed under `unset`. Validate/Save send this same kind-first `BoMGraphMutation` envelope. **Save** is MERGE (`PATCH`); **Overwrite…** is REPLACE (`PUT`) with confirm.

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
| STRING `format` | Optional free text (application-specific); blank omits |
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

