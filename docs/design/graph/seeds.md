# Graph configuration seeds

**Status:** implemented  
**Audience:** product engineers and modeling teams authoring or generating seed YAML  
**Related:** [object-schema-dsl.md](object-schema-dsl.md) (`contentSchema` grammar), [json-schema-to-seeds.md](json-schema-to-seeds.md) (JSON Schema → YAML seeds)

Extensible multi-document YAML for schemas, allowed-edge rules, and graphs. This is the
**portable interchange format** for ontology and instance data. There is no separate
seed-format JSON Schema; validation is performed by the seed handlers and
`BoMSchemaNormalizer`.

## Format

Each YAML document:

```yaml
apiVersion: objs.poc.org/v1
kind: ObjectSchema | AllowedEdgeRule | Graph | *(application-defined)*
# kind-specific fields follow directly at the document root
```

| Field | Required | Notes |
|-------|----------|--------|
| `apiVersion` | **yes** | Only `objs.poc.org/v1` is accepted |
| `kind` | **yes** | Built-in: `ObjectSchema`, `AllowedEdgeRule`, `Graph`. Applications register more via `SeedDocumentHandler` beans. |
| other root keys | kind-specific | Flat at document root — no Kubernetes-style `metadata` / `spec` |

Seed documents deliberately follow Mill's flat format. `apiVersion` and `kind` are the only
common envelope fields.

### Multi-document resources

- Separate documents with `---` (YAML document start markers).
- Empty documents between consecutive `---` markers are **skipped** (canonical SBOM ontology
  often has double `---`; generators may emit a single `---` between docs).
- Documents in one resource may appear in **any order**. The importer validates **all**
  documents first, then applies in `SeedDocumentHandler.applyOrder` (then document index).
  Built-in: `ObjectSchema` (0) → `AllowedEdgeRule` (10) → `Graph` (30). Extra kinds pick a gap
  (asset-repository: `Collection` 20, `CollectionObjects` 40).

Unsupported `apiVersion` or `kind` values fail the **whole** resource (no partial apply).

## Identity and MERGE

All seed import is **MERGE** (upsert). Omission never deletes. There is no `REPLACE` mode in v1.

| Kind | Upsert identity | Notes |
|------|-----------------|-------|
| `ObjectSchema` | `(type, version)` | Re-import updates that catalog row |
| `AllowedEdgeRule` | `(sourceType, role, targetType)` | Re-import updates that allow-list row |
| `Graph` entities / edges | stable textual `key` within the graph document | Default ids are **UUIDv5** over `graphName/entity\|edge/key` in the Objs seed namespace; optional explicit `id` overrides (used by REST export of existing rows) |
| `Graph` header | document `name` (or explicit `id`) | Default graph UUID = UUIDv3 of `graph-seed:<name>` |
| `Collection` (asset-repository) | collection `name` | Creates `ar_collection` + named graph; MERGE updates metadata |
| `CollectionObjects` (asset-repository) | `collection` name | Writes objects/relations into that collection |

Generator implication: re-import updates matching identities; types or rules omitted from a
later import are **not** removed from the catalog or graph by import alone.

## File packaging

Ontology and graph instance I/O are **separated** at the REST boundary:

| File contents | Use for | Import endpoint |
|---------------|---------|-----------------|
| `ObjectSchema` and/or `AllowedEdgeRule` only | Catalog / ontology | `POST /api/v1/objs/registry/import?format=seeds` |
| `Graph` only | Instance data | `POST /api/v1/objs/graph/import?format=seeds` |

Mixed files that include both catalog kinds and `Graph` **fail** endpoint validation.
Startup classpath loading can still list multiple resources (ontology file + graph file).

**Recommendation for ontology seeds:** emit a **catalog-only** multi-doc YAML
(`ObjectSchema` + `AllowedEdgeRule`). Keep `Graph` instance documents in a separate file; do not
mix kinds in one file intended for registry import.

The Schemas workbench **Full schema** overview uses catalog-only export/import and rejects
seed files that contain `Graph` documents.

---

## Kind: `ObjectSchema`

Registers one catalog schema: entity payload or edge-property object.

`contentSchema` uses the authoritative recursive DSL — see
[object-schema-dsl.md](object-schema-dsl.md). Seeds do **not** introduce a second schema
language.

### Document fields

| Field | Required | Default | Notes |
|-------|----------|---------|-------|
| `type` | **yes** | — | Catalog type name; trimmed, nonblank; may contain spaces (e.g. `"Container Image"`) |
| `version` | **yes** | — | Opaque version string (e.g. `"1.0.0"`); trimmed, nonblank |
| `usage` | no | `ENTITY` | Scalar: `ENTITY` or `EDGE_PROPERTIES`. **Not** a list. Omit for entity schemas; export omits when `ENTITY` |
| `contentSchema` | **yes** | — | Must be a root **`OBJECT`** node |
| `tags` | no | absent | Envelope labels; omit when empty |
| `attributes` | no | absent | Envelope string map; omit when empty. `color` is `#rrggbb` for graph nodes, or `nocolor` for theme gray |

### Minimal example

```yaml
apiVersion: objs.poc.org/v1
kind: ObjectSchema
type: Component
version: 1.0.0
contentSchema:
  type: OBJECT
  title: Component
  description: Canonical component payload
  fields: []
```

### Example with attributes

```yaml
apiVersion: objs.poc.org/v1
kind: ObjectSchema
type: Component
version: 1.0.0
attributes:
  color: "#4c6ef5"   # or nocolor for theme gray
contentSchema:
  type: OBJECT
  title: Component
  description: Software component
  fields:
    - name: name
      schema:
        type: STRING
        title: Name
        description: Component display name
      required: true
      identifier: true
      searchable: true
    - name: version
      schema:
        type: STRING
        title: Version
        description: Component version string
      required: false
```

`attributes.color` is reserved for graph node accent: six-digit `#rrggbb`, or `nocolor` for
theme gray. See [`object-schema-dsl.md`](object-schema-dsl.md) (reserved envelope attribute).

### Edge-property schema example

```yaml
apiVersion: objs.poc.org/v1
kind: ObjectSchema
type: CanonicalEdge
version: 1.0.0
usage: EDGE_PROPERTIES
contentSchema:
  type: OBJECT
  title: Canonical edge
  description: Shared edge property bag
  fields:
    - name: createdAt
      schema:
        type: STRING
        title: Created at
        description: Edge creation timestamp
        format: date-time
      required: false
```

### `contentSchema` quick rules (detail in DSL doc)

Every schema **node** needs nonblank `title` and `description` after normalization.

| Node `type` | Extra required children |
|-------------|-------------------------|
| `OBJECT` | `fields` (may be `[]`) |
| `ARRAY` | `items` |
| `ENUM` | nonempty `values: [{value, description, caption?}, …]` — `caption` is optional UI label |
| `STRING` | optional `format` (free text; no allow-list) |
| `NUMBER` / `INTEGER` / `BOOLEAN` | — |

Object **fields**:

| Field | Required | Default | Notes |
|-------|----------|---------|-------|
| `name` | **yes** | — | Unique within the parent object |
| `schema` | **yes** | — | Nested node |
| `required` | no | **`true`** | Emit `required: false` explicitly for optional attributes |
| `identifier` | no | `false` | Scalar leaves only |
| `searchable` | no | `false` | Scalar leaves only |
| `stereotype` | no | absent | UI presentation hints |
| `tags` | no | absent | Field labels; omit when empty |
| `attributes` | no | absent | Field string map; omit when empty |

There is **no** OBJECT-level `required: [...]` list. No `$ref`, composition, nullability, or
bounds in v1.

---

## Kind: `AllowedEdgeRule`

Registers one directed allow-list triple. Relations are **not** embedded inside
`ObjectSchema.contentSchema`.

### Document fields

| Field | Required | Default | Notes |
|-------|----------|---------|-------|
| `sourceType` | **yes** | — | Entity type name, or `*` wildcard |
| `role` | **yes** | — | Relation role, or `*` |
| `targetType` | **yes** | — | Entity type name, or `*` |
| `propertiesPolicy` | no | `NONE` | `NONE` \| `SCHEMA` |
| `emptyPropertiesAllowed` | no | `true` | Boolean |
| `propertiesSchemaType` | if policy=`SCHEMA` | — | Must reference an `EDGE_PROPERTIES` schema `type` |
| `propertiesSchemaVersion` | if policy=`SCHEMA` | — | Matching schema `version` |
| `cardinality` | no | `UNSPECIFIED` | Wire values: `UNSPECIFIED`, `1:1`, `1:*`. Export always emits `cardinality`. Metadata for UI/codegen — **not** persist-enforced |
| `description` | no | absent | What the relation means |
| `sourceVerb` | no | absent | Source → target wording (display only) |
| `targetVerb` | no | absent | Target → source wording (display only) |
| `tags` | no | absent | Labels; omit when empty |
| `attributes` | no | absent | String map; omit when empty |

Upsert key: `(sourceType, role, targetType)`. Verbs and description are **not** part of identity
and are **not** used for matching or persist.

### Example

```yaml
apiVersion: objs.poc.org/v1
kind: AllowedEdgeRule
sourceType: Product
role: CONTAINS
targetType: Component
propertiesPolicy: SCHEMA
propertiesSchemaType: CanonicalEdge
propertiesSchemaVersion: "1.0.0"
emptyPropertiesAllowed: true
cardinality: "1:*"
description: Product includes the component in its bill
sourceVerb: contains
targetVerb: contained in
tags:
  - composition
```

Bare relation (no properties):

```yaml
apiVersion: objs.poc.org/v1
kind: AllowedEdgeRule
sourceType: Person
role: OWNS
targetType: Product
propertiesPolicy: NONE
cardinality: "1:*"
```

---

## Kind: `Graph` (instance data)

Creates/updates one `objs_graph`: header annotations, member entities, and graph-local edges.
Primary for demo/startup data and graph import/export. Ontology-only catalogs omit this kind.

### Document fields

| Field | Required | Default | Notes |
|-------|----------|---------|-------|
| `name` | **yes** | — | Graph seed name; identity for default UUID |
| `id` | no | UUIDv3 `graph-seed:<name>` | Explicit graph UUID |
| `annotations` | no | `{}` | `Map<String,String>` header; non-strings coerced via `toString()` |
| `entities` | **yes** | — | List (key must be present; may be empty) |
| `edges` | no | `[]` | List of edge rows |

### Entity row

| Field | Required | Default | Notes |
|-------|----------|---------|-------|
| `key` | **yes** | — | Stable text; unique within the document |
| `type` | **yes** | — | Must match a registered `ObjectSchema` type (at apply time) |
| `schemaVersion` | **yes** | — | Must match a registered schema version |
| `payload` | no | `{}` | Attribute map; validated against `contentSchema` |
| `annotations` | no | `{}` | `Map<String,String>` |
| `id` | no | UUIDv5 `graphName/entity/key` | Override only when replaying exported ids |

### Edge row

| Field | Required | Default | Notes |
|-------|----------|---------|-------|
| `key` | **yes** | — | Stable text; unique within the document |
| `source` | **yes** | — | **Entity `key` in this document**, not a UUID |
| `target` | **yes** | — | Entity `key` in this document |
| `role` | **yes** | — | Must be allowed by catalog for the source/target types |
| `type` | no | — | Edge-properties schema type when allow-list policy is `SCHEMA` |
| `schemaVersion` | no | — | Matching version when policy is `SCHEMA` |
| `properties` | no | — | Property map when policy is `SCHEMA` |
| `id` | no | UUIDv5 `graphName/edge/key` | Override for exported rows |

### Example

```yaml
apiVersion: objs.poc.org/v1
kind: Graph
name: demo-prod
annotations:
  env: prod
entities:
  - key: p1
    type: Person
    schemaVersion: "1"
    annotations: {}
    payload:
      name: Ada
edges:
  - key: p1-owns-product
    source: p1
    target: product-1
    role: OWNS
```

Each `kind: Graph` document creates/uses one **`objs_graph`**. Entities are attached as members;
edges are stamped with that `graph_id`.

---

## Startup loading

Configuration (`objs.seeds`):

```yaml
objs:
  seeds:
    enabled: true
    on-failure: FAIL_FAST   # or CONTINUE
    resources:
      - classpath:seeds/sbom-ontology.yaml
```

- Locations: `classpath:` and `file:` only.
- The normalized resource location is the ledger key; seed resources need no separate name.
- Ledger table `objs_seed_ledger` stores SHA-256 fingerprints (`sha256:…`).
- Identical successful fingerprints are skipped on restart.
- Failed attempts never overwrite the last successful fingerprint.
- Catalog hydration runs before seed loading. Runtime catalogs use write-through + TTL
  (`objs.catalogs.cache-ttl`); see [persistence.md](persistence.md).

## REST

Ontology and graph instance I/O are **separated**. Both use a required `format` query param
(extensible; `seeds` is the portable YAML format).

| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/api/v1/objs/registry/import?format=seeds` | Multipart YAML; catalog kinds only (`ObjectSchema`, `AllowedEdgeRule`); no ledger write |
| `POST` | `/api/v1/objs/registry/refresh` | Rehydrate catalogs from PostgreSQL (bypass `objs.catalogs.cache-ttl`) |
| `GET` | `/api/v1/objs/registry/export?format=seeds` | Catalog YAML only |
| `GET` | `/api/v1/objs/registry/export?format=json-schema` | Full-catalog JSON Schema (`dialect`, `includeEdges`, `includeEdgePropertySchemas` optional; see object-schema-dsl) |
| `GET` | `/api/v1/objs/registry/export?format=json-schema-codegen` | Same catalog + synthetic root for POJO codegen |
| `POST` | `/api/v1/objs/graph/import?format=seeds` | Multipart YAML; `Graph` kind only (each doc → one `objs_graph`) |
| `GET` | `/api/v1/objs/graph/export?format=seeds` | Requires `graphId`; exports that graph's members/edges |

Former `/api/v1/objs/seeds/**` paths are removed.

## Extension

Register additional Spring beans implementing `SeedDocumentHandler` for new `kind` values.
The importer discovers handlers by kind; export uses the canonical serializer registry.

## Canonical examples

| Resource | Contents |
|----------|----------|
| [`examples/sbom/sbom-service/.../seeds/sbom-ontology.yaml`](../../../examples/sbom/sbom-service/src/main/resources/seeds/sbom-ontology.yaml) | Catalog: many `ObjectSchema` + `AllowedEdgeRule` |
| [`examples/sbom/sbom-service/.../seeds/sbom-demo-graph.yaml`](../../../examples/sbom/sbom-service/src/main/resources/seeds/sbom-demo-graph.yaml) | Instance: `kind: Graph` docs |

Canonical ontology YAML (`classpath:seeds/sbom-ontology.yaml`) is the registry source of truth at
runtime under the `sbom` Spring profile. Typed `SbomRegistry.pack()` remains for builders and
parity tests.

## Ground truth (when docs and behaviour disagree)

Prefer the Kotlin handlers and normalizer over older work-item text:

| Concern | Code |
|---------|------|
| ObjectSchema parse/serialize | `org.poc.objs.core.seed.ObjectSchemaSeedHandler` |
| AllowedEdgeRule parse/serialize | `org.poc.objs.core.seed.AllowedEdgeRuleSeedHandler` |
| Graph parse/apply | `org.poc.objs.core.seed.GraphSeedHandler` |
| `contentSchema` strict rules | `org.poc.objs.core.domain.BoMSchemaNormalizer` |
| Envelope constants | `org.poc.objs.core.seed.SeedModels` (`SEED_API_VERSION_V1`, kind names) |
| Import orchestration | `org.poc.objs.core.seed.SeedImporter` |

---

## Appendix: Seed generation checklist (catalog)

Use this when emitting ontology YAML (including from JSON Schema — see
[json-schema-to-seeds.md](json-schema-to-seeds.md)). For a pasteable Python emitter, see
[`tools/objs_seed.py`](../../../tools/objs_seed.py) and the how-to section in
[json-schema-to-seeds.md](json-schema-to-seeds.md#python-nano-framework-toolsobjs_seedpy).

1. **Emit catalog kinds only** for registry import: `ObjectSchema` + `AllowedEdgeRule`.
2. Every document: `apiVersion: objs.poc.org/v1` and a valid `kind`.
3. Separate documents with a single `---` (empty docs between markers are tolerated but noisy).
4. One `ObjectSchema` document per `(type, version)`.
5. Map attributes to `contentSchema.fields`: each field needs `name`, nested `schema` with
   `type` / `title` / `description`, and explicit `required: true|false` (default is **true** if
   omitted).
6. Every nested node (including ARRAY `items` and ENUM) needs nonblank `title` and `description`.
7. Use DSL node types: `STRING`, `NUMBER`, `INTEGER`, `BOOLEAN`, `ENUM`, `OBJECT`, `ARRAY` —
   not raw JSON Schema keywords inside `contentSchema`.
8. Do **not** emit `$ref`, `oneOf` / `anyOf` / `allOf`, nullable unions, pattern/minLength, numeric
   bounds, or `additionalProperties: false`. Flatten or redesign the source schema first.
9. Do **not** put relations inside `contentSchema`. Emit **`AllowedEdgeRule`** documents instead.
10. For shared edge attributes, emit an `ObjectSchema` with `usage: EDGE_PROPERTIES`, then reference
    it from rules via `propertiesPolicy: SCHEMA` + `propertiesSchemaType` / `Version`.
11. `usage` is a **scalar**; never `usages: [...]`.
12. Quote type names that contain spaces; quote versions consistently as strings.
13. Prefer stable, human-readable `type` and `role` names; MERGE identity depends on them.
14. Validate by importing: `POST /api/v1/objs/registry/import?format=seeds`, or by feeding YAML
    through the same handlers/normalizer in tests.
15. Remember MERGE: omitting a type or rule from a re-import does not delete the catalog entry.
16. Do **not** reverse-engineer seeds from `format=json-schema` export — that projection is
    lossy and may synthesize relation properties that are not part of the authoring model
    (see [json-schema-to-seeds.md](json-schema-to-seeds.md)).

## Extending seed kinds

Built-in handlers are Spring beans implementing `SeedDocumentHandler`. An application can add
kinds the same way: `@Component` with a unique `kind`, `parse` + `apply`, and `applyOrder` in a
gap (do not collide with 0 / 10 / 30). Duplicate `kind` values fail importer construction.

Classpath startup (`objs.seeds.resources`) applies **all** registered kinds. REST import still
filters: registry = catalog kinds only; graph import = `Graph` only. Application kinds are for
startup (or a domain import endpoint), not the foundation registry/graph I/O split.

