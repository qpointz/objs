# Story: Catalog schema metadata (edges, format, tags)

**Slug:** `catalog-schema-metadata`  
**Branch:** `catalog-schema-metadata`  
**Status:** closed  
**Folder:** [`docs/workitems/completed/20260818-catalog-schema-metadata/`](.)  
**Backlog:** [C-16](../../BACKLOG.md) (done)  
**Base:** `origin/dev`  
**Design:** [`docs/design/graph/object-schema-dsl.md`](../../../design/graph/object-schema-dsl.md), [`docs/design/graph/seeds.md`](../../../design/graph/seeds.md), [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Give catalog authors application-specific metadata on **object schemas** and **allowed-edge rules**, and stop treating STRING `format` as a closed list. Seeds and the workbench edit forms must round-trip the new fields.

## Normative locks

| Topic | Lock |
|-------|------|
| Allowed-edge extra fields | Optional `description`, `sourceVerb` (source → target), `targetVerb` (target → source). Identity remains `(sourceType, role, targetType)`. Verbs are **not** used for matching or persist |
| STRING `format` | Optional free text (trimmed; blank → omit). Drop the allow-list. Still copied to JSON Schema `format` when set |
| Tags | `tags: List<String>` — trim, drop blanks, de-dupe, preserve order |
| Attributes | `attributes: Map<String, String>` — string values only |
| Where tags/attributes apply | Object schema **envelope**; each **field**; each **allowed-edge rule**. Not nested OBJECT nodes |
| `stereotype` | Unchanged (UI hints such as `multiline`). Do **not** merge with `tags` |
| Persistence | `bom_*` via objs-core Flyway **V2** (`postgresql` **and** `h2`). Envelope tags/attributes are columns on `bom_entity_schema` (`definition_doc` stays the `contentSchema` node). Edge metadata is columns on `bom_edge_schema`. Field tags/attributes live in the DSL JSON |
| Inclusion | Seeds (`ObjectSchema`, `AllowedEdgeRule`) + registry REST + workbench forms. Empty collections omitted on export |
| Examples | Same story: SBOM (and AR if it ships catalog seeds) round-trip; a few canonical edges get real verbs/descriptions |
| Example schema browse | SBOM + AR read-only schema pages show **Allowed edges** for the object type (WI-005). List the same facts as workbench schema-edges; use example UI styling, not workbench chrome |
| ENUM `caption` | Optional UI label on each enum value. `value` = stored token; `description` = long meaning (required); `caption` = short UI text. Blank/omit → UI shows `value`. Object editors (workbench + examples) use caption, never description, as the dropdown label |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | Story folder, GAPS, trackers |
| 1 — Design lock | WI-001 | done | DSL / seeds / persistence living docs |
| 2 — objs-core | WI-002 | done | Domain, Flyway V2, JPA, seed handlers |
| 3 — REST + workbench | WI-003 | done | Registry API + edit forms |
| 4 — Examples + docs polish | WI-004 | done | SBOM/AR seeds; remaining living-doc nits |
| 5 — Example schema UI | WI-005 | done | Read-only allowed edges on SBOM + AR schema browse |
| 6 — Enum captions | WI-006 | done | Optional enum `caption`; object editors in workbench + examples |

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock: DSL, seeds, persistence (`WI-001-design-lock.md`)
- [x] WI-002 — objs-core domain, Flyway V2, JPA, seeds (`WI-002-core-metadata.md`)
- [x] WI-003 — Registry REST + workbench edit forms (`WI-003-rest-and-workbench.md`)
- [x] WI-004 — Example seeds + living docs (`WI-004-examples-and-docs.md`)
- [x] WI-005 — Example schema browse: read-only allowed edges (`WI-005-example-schema-allowed-edges.md`)
- [x] WI-006 — Enum value captions (`WI-006-enum-captions.md`)

## Out of scope

- Using verbs in allow-list matching or persist  
- JSON / nested attribute values  
- Tags on nested OBJECT nodes (only envelope + fields)  
- Changing `stereotype` semantics  
- History repair for existing DBs (additive columns; greenfield still the Flyway contract)  
- D-6 transactional Save / D-7 file demo inventory  
- Editing allow-list rules from example apps (workbench only)  
