# WI-004 — Deep graph version

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Deep freeze  
**Status:** done  
**Depends on:** WI-003  
**Examples:** **—**

## Goal

Add **`createDeepGraphVersion`** (product Snapshot). Pin current member **and** edge versions (`BIGINT`). Reconstruct from `*_version`. Live GET unchanged.

**Do not remove `clone()`.** Clone stays C-12 deep copy: new graph + new entity/edge ids from current HEAD. Freeze is a different API.

## Schema

- `bom_graph_version_member (graph_id, graph_version, entity_id, entity_version)`
- `bom_graph_version_edge (graph_id, graph_version, edge_id, edge_version)`
- Composite FKs to `*_version`. Shallow header persist = graph version **without** children.

## API

- `createDeepGraphVersion(graphId, versionAnnotations)` — one TX: copy **current** HEAD of graph + members + edges into new `*_version` rows (`nextVersion`), set each `head_version`, insert pin children (`kind=deep`). Same `graph_id`. First freeze creates the first version rows.
- `getGraphVersion(graphId, version)` — reconstruct (slow OK). Works after Delete HEAD.
- `listGraphVersions(graphId)` — newest first (`version DESC`); for Explorer pane.
- **Keep** `clone(sourceId, annotations)`. REST: keep `POST /graphs/{id}/clone`. Add `POST /graphs/{id}/versions` (or `/snapshot`) for freeze; `GET /graphs/{id}/versions`; `GET /graphs/{id}/versions/{version}`.

## Clone after version tables exist

- Copies **current HEAD** only (payloads, membership, remapped edges).
- New ids; source graph and its `*_version` rows unchanged.
- Does **not** copy source `*_version` / pin children.
- New HEAD rows: `head_version` NULL; `*_version` count 0 on the clone until **that** graph’s own Snapshot.
- After freeze-then-edit-HEAD, clone still copies live HEAD (not the freeze).

## Tests

- Live in-place edit after freeze does not change reconstruct; no extra version row until next freeze
- Zero extra `bom_entity` identities on freeze (version rows yes)
- Edges included at pin-time properties
- `copyGraph` after freeze is live HEAD
- Delete HEAD; reconstruct still loads
- `clone()` still new ids; clone `*_version` empty; source versions untouched
- Clone after freeze+HEAD-edit copies post-edit HEAD, not pin-time payloads
- Example modules still compile/test; SBOM/AR live paths unchanged until WI-006 rewires fingerprint
