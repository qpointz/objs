# WI-006 — SBOM fingerprint

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 6 — SBOM  
**Status:** done  
**Depends on:** WI-004  
**Examples:** **SBOM**

## Goal

Keep-split draft: `copyGraph`. Combine-on-new-draft: `mergeGraph`. Fingerprint: **`createDeepGraphVersion`** on the Combined union’s graph (or persist union then deep-freeze). Store `(graph_id, graph_version)` not a new snapshot graph. Asset inventory = HEAD. Open fingerprint = `getGraphVersion(graphId, version)`.

## Tests

- Edit live asset after fingerprint → fingerprint GET still pin-time payload (entities **and** edges)
- No extra pool identities
- Draft-from-fingerprint = `copyGraph` of **live** HEAD (does not restore freeze)
- Writes to fingerprint reconstruct remain forbidden (freeze is read-only)

Drop unused `materialize` / clone-style fingerprint helpers. Store `clone()` is **kept** (not used for fingerprints).

**Examples:** `:sbom-service:test` (and UI if freeze UI changes) green. Inventory, drafts, Combined GET, keep-split, combine still work. `:asset-repository-service:test` still green (untouched freeze product).
