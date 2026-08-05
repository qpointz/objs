# Objs UI user manual

The Objs workbench is a browser UI for exploring stored graphs, inspecting registered schemas,
and authoring object-schema DSL definitions.

## Start and open

Start `objs-app`, then open:

```text
http://localhost:8080/workbench/
```

For PostgreSQL:

```powershell
docker compose -f deploy/local-dev/docker-compose.yml up -d
./gradlew :objs-app:run --args="--spring.profiles.active=postgres,sbom"
```

The **top header** shows a subtle **Workbench** brand (`IconTournament`) and three views:

| View | Path | Purpose |
|------|------|---------|
| **Explorer** | `/workbench/explorer` | Query and inspect a stored subgraph |
| **Composer** | `/workbench/composer` | Draft workspace: load, Visual/Text edit, Validate / Apply mutation |
| **Schema** | `/workbench/model` | Browse and edit object/edge schemas |

Legacy `/ui/**` URLs redirect into `/workbench/**` (e.g. `/ui/graph` → `/workbench/explorer`).

## Graph explorer

Graph explorer loads entities and induced edges through
`POST /api/v1/objs/graph/query`. Matcher controls are shared with Object linter
(`MatcherQueryForm`): a compact **Matcher** select (`anno` / `anno-expr` / `chained`) plus mode
fields, with **Exec** on the same row.

- **`anno`** — key/value rows. Every pair must match the entity annotation map.
- **`anno-expr`** — one JEXL Boolean expression using annotation keys as variables,
  for example `app == 'payments-api' && appVersion == '2.3.1'`.
- **chained** — non-empty JSON array of matcher objects. Matchers execute in order, passing
  each stage's selected entities to the next.

The UI sends the selected mode as matcher DSL:

```json
[
  { "anno": { "app": "payments-api" } },
  { "anno-expr": "appVersion == '2.3.1'" }
]
```

1. Configure the matcher.
2. Select **Exec** (shows a loading overlay while the query runs).
3. Select a node or edge on the canvas to inspect it. Edge source/target links jump to that node.
4. Select **Apply layout** to recalculate the graph layout.
5. After a successful query, **Edit in linter** opens Object linter and loads the same matcher into
   the draft (reuses the Load overwrite confirm when a draft already exists).

The last successful matcher is kept in `localStorage` (`objs.ui.graphExplorer.matcher`). The last
executed graph, layout direction, node positions, and query id are kept in
`objs.ui.graphExplorer.session` so navigating away (e.g. to Schemas) and back restores the canvas.
A new **Exec** replaces that session; a failed **Exec** clears it.

After **Exec**, the matcher row shows wall-clock query time plus node/edge counts.

### Selection history

Node and edge selection is stored in the URL (`?qid=<uuid>&node=<id>` or `&edge=<id>`). Each
successful **Exec** mints a new `qid` (also persisted in the session). Browser **Back** / **Forward**
restores selection only when the URL `qid` matches the current result set; otherwise the inspector
clears. Entity-type badges above the graph open that type in Schema explorer.

### Inspect a node

Selecting a node shows:

- entity name, type, schema version, and ID;
- annotations used for graph selection;
- JSON payload.

Select **Open object model in Schema explorer** to inspect the exact schema version used by the
entity.

### Inspect an edge

Selecting an edge shows:

- role, type, schema version, and ID;
- source and target IDs;
- edge properties.

When the edge has a property schema, select **Open edge property schema** to inspect it.

## Schemas

Schemas is a single workbench for browsing and editing catalog types.

### Full schema (overview)

Opening **Schemas** without a type selected shows the **Full schema** overview:

- ontology graph of all **ENTITY** object types and allow-list edges (wildcard `*` as one node);
- **Visual** / **Text** tabs: Visual shows the ontology graph; Text is a read-only catalog export with
  a **JSON Schema** / **Seeds** segmented control (same pattern as the type editor JSON/YAML toggle);
- click a type node (or a row in the type list) to open that type’s latest version;
- nodes are draggable; positions and layout direction are kept in `localStorage`
  (`objs.ui.fullSchema.layout`) and restored on return (new types still use auto layout until moved);
- **Apply layout** (with direction menu: TB / LR / BT / RL) re-runs automatic layout and clears
  saved node positions;
- **Export** menu downloads either catalog seed YAML
  (`GET /api/v1/objs/registry/export?format=seeds`) or full-catalog JSON Schema
  (`…?format=json-schema`);
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
- **All** — both usages.

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
usages: [ENTITY]
contentSchema:
  type: OBJECT
  title: Component
  description: Component payload
  fields: []
```

For an edge-property schema, use `usages: [EDGE_PROPERTIES]` and the same `contentSchema` shape.
Allowed edge rules are managed on the object **Edges** tab (and registry edge endpoints), not in
the YAML/JSON document.

For a new draft, type, version, usages, and content definition can all be edited directly.
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
2. choose one or both usages;
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

## Object linter

Object linter is a **graph draft workspace**: optionally load a stored subgraph, manipulate it
visually or as YAML/JSON, then **Validate** or **Apply**.

| Action | API |
|--------|-----|
| Load | `POST /api/v1/objs/graph/query` (matcher DSL: `anno` / `anno-expr` / chained) |
| Validate | `POST /api/v1/objs/graph/validate` with `BoMGraphMutation` |
| Apply | `PUT /api/v1/objs/graph` with the same mutation body (`upsert` + `delete`) |

**New UUID** (clipboard + toast, auto-hides after 3s) sits on the Text tab toolbar next to Format /
Rollback.

### Tabs

| Tab | Role |
|-----|------|
| **Visual** | React Flow canvas with inline side panel; toolbar: add / create linked / connect (Ctrl+click two nodes) / delete / layout; edit form only for a single selection |
| **Text** | YAML/JSON of the **mutation only** (`upsert.entities` / `upsert.edges` + `delete.entities` / `delete.edges` ids). Unchanged loaded objects stay on Visual but are omitted from Text until edited, created, or deleted. |

Invalid Text blocks switching to Visual; the last good draft is preserved.

### Load / pending deletes

1. Open **Load…** and choose a matcher (same compact `MatcherQueryForm` as Graph explorer).
2. Confirm replace when the draft is non-empty.
3. Loaded entity/edge ids become the **baseline**. Removals from the draft become **pending deletes** for Validate/Apply.
4. After Load with no edits, Text is an empty mutation; Visual still shows the loaded graph.
5. Graph explorer **Edit in linter** navigates here with the last successful matcher and triggers the same Load path.
6. Successful Load shows query wall time and counts; selection uses the same `qid` + `node`/`edge`
   URL history as Explorer (scoped to that Load).
7. On the Visual canvas, draft status icons appear on object headers: **+** new, **pencil** modified, **−** deleted.
8. Deleting a **new** (non-baseline) object/edge removes it from the canvas. Deleting a **loaded** object/edge soft-deletes it: it stays visible and marked deleted until Apply, and can be undone (restore) or have modifications reverted from the side panel.
9. **Reset** restores the last load/rollback snapshot; **Clear** empties the draft.

Optional **Copy annotations from source** when creating a linked object.

### Validate and Apply

1. Open **Object linter**.
2. Optionally Load a subgraph, or start from an empty draft in Visual/Text.
3. Select **Validate** (dry-run mutation) or **Apply** (persist mutation).

Apply clears pending deletes and refreshes the baseline on success.

```yaml
upsert:
  entities: []   # creates / updates only
  edges: []
delete:
  entities: []   # loaded entity ids
  edges: []      # loaded edge ids
```

Loaded baseline objects appear in Text only after they are modified, newly created, or listed under `delete`. Validate/Apply send this same `BoMGraphMutation` envelope.

Entities that are referenced by mutation edges need explicit UUIDs. Edge source/target types, role,
property-schema reference, and properties are validated against the current registry.

## Common errors

| Message or condition | Resolution |
|----------------------|------------|
| Graph query returns no nodes | Verify `anno` keys/values or `anno-expr` variables exist on stored entities and that chained stages retain results |
| Matcher query is rejected | Correct the matcher shape: one `anno`/`anno-expr` object or a non-empty JSON array of matcher objects |
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
Set-Location objs-service/ui
npm install
npm run dev
```

Open `http://localhost:5173/workbench/`. Vite proxies `/api` requests to
`http://localhost:8080`, so `objs-app` must also be running.

Packaged builds: Gradle `:objs-service` runs `npm run build` and syncs dist into
`classpath:/static/ui/` (skip with `-PskipUi=true`). The SPA is served by
`:objs-service` at `/workbench/` and does **not** require `:objs-sbom-example`.

