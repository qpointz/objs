# Objs UI user manual

The Objs workbench is a browser UI for exploring stored graphs, inspecting registered schemas,
and authoring object-schema DSL definitions.

## Start and open

Start `objs-app`, then open:

```text
http://localhost:8080/ui/
```

For PostgreSQL:

```powershell
docker compose -f deploy/local-dev/docker-compose.yml up -d
./gradlew :objs-app:run --args="--spring.profiles.active=postgres,sbom"
```

The **top header** contains three views (no left app navbar):

| View | Purpose |
|------|---------|
| **Graph explorer** | Query and inspect a stored subgraph |
| **Schemas** | Browse and edit object/edge schemas (unified workbench) |
| **Object linter** | Validate a YAML/JSON graph draft without persistence |

Legacy `/ui/linter` URLs redirect into Schemas.

## Graph explorer

Graph explorer loads entities and induced edges through
`POST /api/v1/objs/graph/query`. Select one matcher mode:

- **`anno`** — add/remove Mantine key/value rows. Every pair must match the entity annotation map.
- **`anno-expr`** — enter one JEXL Boolean expression using annotation keys as direct variables,
  for example `app == 'payments-api' && appVersion == '2.3.1'`.
- **chained** — enter a non-empty JSON array of matcher objects. Matchers execute in order, passing
  each stage's selected entities to the next.

The UI sends the selected mode as matcher DSL:

```json
[
  { "anno": { "app": "payments-api" } },
  { "anno-expr": "appVersion == '2.3.1'" }
]
```

1. Configure the selected matcher mode.
2. Select **Exec**.
3. Select a node or edge on the canvas to inspect it.
4. Select **Apply layout** to recalculate the graph layout.

The result summary shows the number of nodes and edges. Entity-type badges above the graph open
that type in Schema explorer.

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

- Version selector and **Create version** (next major).
- **Save update** for the opened version.
- New drafts use **Create schema**.
- **Lint** lives on the Expert tab toolbar (with Format / Rollback).

### Editors

Tab order: **Visual**, **Schema**, **Expert**, **JSON Schema**.

- **Visual** — read-only relationship graph of allow-list neighbours (ego view for the selected type).
- **Schema** — recursive content-schema tree editor.
- **Expert** — full schema document JSON/YAML; **Format**, **Rollback**, and **Lint**.
- **JSON Schema** — generated projection.

Unsaved edits show an **Unsaved changes** badge with **Rollback** to the last loaded/saved
snapshot. Switching type, version, create draft, or leaving Schemas opens a confirmation dialog
(Stay / Leave). Browser close/reload is also blocked while dirty.

### Allowed edges (objects)

Below the content editors, object schemas show the **Allowed edges** table (inbound then outbound).
You can **add**, **edit**, and **delete** rules (direction, related type, role, cardinality,
properties NONE or SCHEMA). Edge edits stay local until **Save update** (with content-schema
edits); **Rollback** restores both. Editing identity fields (source / role / target) replaces the
draft rule. Edge-property schemas edit payload DSL only — relations are not authored on the edge
model.

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
- content editors (**Visual**, **Schema**, **Expert**, **JSON Schema**);
- an **Allowed edges** table below the editors (incoming first, then outgoing), with direction icons
  (`IconArrowNarrowLeftDashed` / `IconArrowNarrowRightDashed`);
- generated JSON Schema 2020-12.

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

- **Edit / lint** opens the selected version in Schema linter. **Save update** replaces that same
  version.
- **Create version** opens the selected definition as the starting point for a new major version.

Existing versions are not implicitly changed when creating a version.

## Schema linter

Schema linter supports two synchronized editing modes (now under Schemas as **Schema** / **Expert**):

- **Schema** — recursive schema tree and field/node editor;
- **Expert JSON/YAML** — direct editing of the complete schema document.

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

### Expert JSON/YAML mode

Expert mode supports YAML and JSON and exposes the complete authoring document:

- **Format** reformats the current document.
- **Rollback** restores the last loaded or saved schema snapshot (also available next to
  **Unsaved changes** outside Expert mode).
- Switching back to Schema mode parses the current document. Invalid source remains in Expert mode
  and shows an error.

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

For an edge-property schema, use `usages: [EDGE_PROPERTIES]` and include its relations:

```yaml
allowedRelations:
  - sourceType: Product
    role: CONTAINS
    targetType: Component
    emptyPropertiesAllowed: true
```

For a new draft, type, version, usages, content definition, and allowed relations can all be edited
directly. When editing an existing schema, expert mode requires type and version to remain equal to
the opened catalog entry; use **Create version** for a new version.

### Lint

Select **Lint** to validate without saving. The server:

1. parses the DSL into the typed schema model;
2. validates and normalizes the complete recursive definition;
3. generates a JSON Schema preview.

A successful result shows **valid**, the normalized definition, and generated JSON Schema. An
invalid result shows an issue code and message. Lint never persists changes.

### Save update

When an existing version is opened, **Save update** validates and replaces that exact
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

From an existing schema, **Create version** opens a draft. After editing it, select
**Save new version** to create the next major version based on all versions currently registered
for that type:

- `1`, `2`, `4` creates `5`;
- `1.0.0`, `2.3.1`, `4.2.0` creates `5.0.0`.

Minor and patch increment workflows are not implemented. Existing versions are never overwritten
by this action. For an edge-property schema, cloned relations are moved to the new schema version
because each source–role–target rule has one active property-schema reference.

## Object linter

Object linter validates a complete graph batch without writing entities or edges to the database.
It calls `POST /api/v1/objs/graph/validate`, which always returns a validation result and does not
persist the submitted graph.

The global **New UUID** action in the workbench header is available from every view. It generates a
UUID, copies it to the clipboard, and displays the value in a notification for use in schemas,
entity or edge IDs, and other authored content.

1. Open **Object linter**.
2. Choose YAML or JSON in the source editor.
3. Define `entities` and `edges`.
4. Select **Validate graph**.

The result reports **valid** or lists each issue with its code, message, and graph path.

```yaml
entities:
  - id: 11111111-1111-4111-8111-111111111111
    type: Product
    schemaVersion: 1.0.0
    payload:
      name: Payments API
      version: 2.3.1
    annotations: {}
  - id: 22222222-2222-4222-8222-222222222222
    type: Component
    schemaVersion: 1.0.0
    payload:
      name: payment-core
      version: 2.3.1
      ecosystem: maven
      kind: library
    annotations: {}
edges:
  - source: 11111111-1111-4111-8111-111111111111
    target: 22222222-2222-4222-8222-222222222222
    role: CONTAINS
    type: CanonicalEdge
    schemaVersion: 1.0.0
    properties: {}
```

Entities that are referenced by draft edges need explicit UUIDs. Entities without IDs can be
validated when no draft edge references them. Edge source/target types, role, property-schema
reference, and properties are validated against the current registry.

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
Set-Location objs-sbom-example/ui
npm install
npm run dev
```

Open `http://localhost:5173/ui/`. Vite proxies `/api` requests to
`http://localhost:8080`, so `objs-app` must also be running.

