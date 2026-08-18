# Gaps — catalog-schema-metadata (C-16)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

Locks below are the story contract. Living design is updated in WI-001.

---

## Architecture

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-A1 | Allowed-edge wording | **resolved** | Optional `description`, `sourceVerb` (source → target), `targetVerb` (target → source). YAML/JSON/API names are camelCase as listed |
| G-A2 | Verbs vs identity | **resolved** | Identity stays `(sourceType, role, targetType)`. Verbs never participate in matching or persist |
| G-A3 | STRING `format` | **resolved** | Free text. Drop `allowedStringFormats`. Blank/whitespace → `null`. JSON Schema projection still emits `format` when set; unknown formats are application-specific |
| G-A4 | Tags | **resolved** | `List<String>`: trim, drop blanks, de-dupe, preserve first-seen order |
| G-A5 | Attributes | **resolved** | `Map<String, String>` only. Duplicate keys: last wins on parse. Empty map omitted on export |
| G-A6 | Placement | **resolved** | Envelope (`BoMSchema`) + field (`BoMSchemaField`) + allowed-edge rule. Nested OBJECT **nodes** do not get their own tags/attributes |
| G-A7 | `stereotype` vs `tags` | **resolved** | Keep `stereotype` for UI hints. `tags` is a separate author-defined label list |
| G-A8 | Envelope storage | **resolved** | `tags` / `attributes` columns on `bom_entity_schema`. Do **not** wrap `definition_doc` (it remains the `contentSchema` node) |
| G-A9 | Edge storage | **resolved** | Columns on `bom_edge_schema`: `description`, `source_verb`, `target_verb`, `tags`, `attributes` |
| G-A10 | Field storage | **resolved** | Inside DSL JSON in `definition_doc` |
| G-A11 | Flyway | **resolved** | objs-core `V2` for **postgresql** and **h2** in the same WI (RULES **Flyway (library + derived apps)**) |
| G-A12 | JSON Schema extras | **resolved** | Do **not** emit `x-objs-tags` / `x-objs-attributes`. Tags/attributes are catalog metadata; `format` remains the only JSON Schema projection of this story |
| G-A13 | Graph edge labels | **resolved** | Workbench may show `sourceVerb` as a display label when set; stored/API identity remains `role` |
| G-A14 | ENUM UI label | **resolved** | Name is **`caption`** (not label/name). Optional. `value` = persisted token; `description` = required long text; `caption` = short UI text. Object editors (workbench + example instance forms) show `caption` if non-blank, else `value`. Do **not** use `description` as the dropdown label. JSON Schema: `x-objs-enumCaptions` only for values that have a caption |

---

## Example schema browse (WI-005)

SBOM and AR schema pages (`SchemaViewPage`) are near-duplicates: read-only `contentSchema` tree + JSON/YAML. Workbench `ObjectEdgesEditor` is the **information** reference for allow-list rows.

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-E1 | Where | **resolved** | Both example schema **detail** pages. Not the catalog grid/list. Not related-assets instance UI |
| G-E2 | Style | **resolved** | Match example Mantine (titles, dimmed text, bordered table/papers like schema cards). Do **not** copy workbench editor chrome, graph, or edit/delete controls |
| G-E3 | Shared package | **resolved** | Keep the existing copy-both-UIs pattern. No new shared frontend library this story. Behaviour must stay in parity |
| G-E4 | What to list | **resolved** | Same facts as workbench edges table: direction (in/out), source type, role, target type, cardinality (wire: `UNSPECIFIED` / `1:1` / `1:*`), properties policy, properties schema type@version when `SCHEMA`. Plus this story’s `description`, `sourceVerb`, `targetVerb`, `tags`, `attributes` when non-empty |
| G-E5 | Matching | **resolved** | Inbound = `targetType` is this type or `*`. Outbound = `sourceType` is this type or `*`. Same as workbench `edgesForType` (SBOM `relationshipsForType` today misses wildcards — do not reuse that as-is) |
| G-E6 | JSON/YAML | **resolved** | Allowed edges are **not** embedded in the schema document dump. Section is page chrome (always available on the detail page), independent of Visual/JSON/YAML |
| G-E7 | Links | **resolved** | Source/target type names link to `/schemas/{type}` when that type is in the example catalog |
| G-E8 | Empty | **resolved** | Dimmed empty copy when no rules match (do not hide the section) |
| G-E9 | API | **resolved** | Example **domain** read APIs (not workbench `/api/v1/objs/**`). Expose the G-E4 fields. SBOM may add/extend a schema-browse endpoint rather than overloading related-assets `relationshipsForType`. AR gets the same shape on its schema API |

---

## Open

_(none — remaining work is implementation)_

---

## Out of story

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-X1 | JSON / object attribute values | **cancelled** | String map only |
| G-X2 | Verbs in persist matching | **cancelled** | Display / documentation metadata |
| G-X3 | Tags on nested OBJECT nodes | **cancelled** | Envelope + fields only |
| G-X4 | `stereotype` redesign | **cancelled** | Unchanged |
| G-X5 | Transactional inventory Save | **deferred** | Backlog **D-6** |
| G-X6 | File-based demo inventory | **deferred** | Backlog **D-7** |
| G-X7 | Edit allow-list from example apps | **cancelled** | Workbench / WI-003 only |
