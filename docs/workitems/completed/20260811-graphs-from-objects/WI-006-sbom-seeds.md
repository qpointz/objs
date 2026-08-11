# WI-006 — SBOM example + seeds

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Example + docs  
**Status:** done  
**Depends on:** WI-005 (**stage 3 confirmed**)  
**Modules:** `:objs-sbom-example`, seeds, `:objs-app` if needed

## Goal

SBOM example uses **graphs** (membership + graph-local edges), not a global entity soup. Required per RULES § Concrete example integration.

Any app-level snapshot lineage (annotations, etc.) stays in the example — **not** foundation columns.

## Touch

- Seeds: create `bom_graph`; attach entities; edges with `graph_id`
- Services/builders/demos: graph context
- Matchers in tests/examples: `graph-expr` / `obj-expr` / chained only
- Tests for example path
- `docs/design/sbom/` if behaviour changes

## Acceptance

- [x] Example boots and demos on graph model
- [x] Seeds match rename map
- [x] No reliance on retired matcher keys
- [x] STORY `[x]`; commit; push

## Commit message hint

`[feat] SBOM example uses bom_graph (WI-006)`

## Note

Stage 4 gate is after **WI-007** as well.

## Implementation notes

- `SbomService`: one `bom_graph` per `(app, appVersion)` (deterministic id); save via `BoMSubgraphStore.mutate`; fetch via `graph-expr` (+ optional chained `obj-expr`); list apps from graph headers.
- `GraphSeedHandler`: each `kind: Graph` document creates/uses a `bom_graph` (optional `id` / `annotations`); edges stamped with `graph_id`.
- Demo seed split into payments + billing Graph documents; enabled under `sbom` profile.
- Bumped `jackson-annotations` catalog pin to 2.21 (Jackson 3 / Flyway classpath).

