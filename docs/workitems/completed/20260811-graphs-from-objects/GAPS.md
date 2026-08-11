# Gaps — graphs-from-objects

Normative: [`STORY.md`](STORY.md).

| ID | Topic | Status | Notes |
|----|-------|--------|-------|
| G-G1 | No global graph | **resolved** | Pool ≠ graph |
| G-G2 | Entity ∈ 0..n graphs | **resolved** | M2M `bom_graph_entity` |
| G-G3 | Orphans | **resolved** | |
| G-G4 | Edges graph-local | **resolved** | `graph_id` NOT NULL |
| G-G5 | Clone | **resolved** | Optional; no lineage on `bom_graph` |
| G-G5a | Snapshot hierarchy | **app-level** | Not foundation |
| G-G5b | Packs wording | **resolved** | Say **graph** |
| G-G6 | Table renames | **resolved** | See STORY map |
| G-G7 | Graph delete | **resolved** | Membership + edges CASCADE; entities kept |
| G-G8 | REST `/graphs` | **resolved** | + entity pool |
| G-G9 | Stage gates | **resolved** | Manual confirm each stage |
| G-G10 | Cleanup | **resolved** | WI-008: dead pack/selector/anno-matcher stack removed; Kotlin + UI renames; design doc → `annotations-and-matchers.md` |
| G-G11 | Edge endpoints | **resolved** | Both members of edge’s graph |
| G-G12 | Kotlin renames | **resolved** | `BoMNamedGraphStore` / `BoMGraphContents` / `GRAPH_*` codes; UI `OpenGraphModal` / `NewGraphModal` |
| G-G13 | Workbench context | **resolved** | Current graph required |
| G-G15 | Minimal matchers | **resolved** | `all`, `graph-expr`, `obj-expr`, chained |
| G-G16 | Bare `obj-expr` | **resolved** | Fail closed without graph scope |
| G-G17 | Retire old keys | **resolved** | Drop `anno`, `anno-expr`, `ids`, `subgraph`, `subg-expr` |

## Deferred

- Snapshot genealogy in objs schema/UI
- Object content versioning
- Auto-version graphs on shared entity edit
- Long-lived legacy matcher compat

## Implementer notes

- Fail closed on membership / endpoints / bare `obj-expr`.
- Clone one TX; no parent FK.
- `graph-expr` bindings: `id`, `a` · `obj-expr`: `id`, `type`, `schemaVersion`, `a`, `p`.
- No TinkerPop in `:objs-core`.
- Update `:objs-sbom-example` in WI-006.
