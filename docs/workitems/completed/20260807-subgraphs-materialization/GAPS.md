# Gaps — subgraphs-materialization

Normative decisions live in [`STORY.md`](STORY.md). This table is the cold-start checklist of locks and open items.

| ID    | Topic                           | Status       | Notes |
| ----- | ------------------------------- | ------------ | ----- |
| G-S1  | Membership shape                | **resolved** | `bom_subgraph` + `bom_subgraph_entities` + `bom_subgraph_edges`; soft links; same entity/edge UUIDs |
| G-S2  | Edge membership                 | **resolved** | Explicit edges; both endpoints must already be entity members |
| G-S3  | Composer open behaviour         | **resolved** | **Replace** draft when opening a pack. Merge out of scope for v1 |
| G-S4  | REST shape                      | **resolved** | CRUD `/graph/subgraphs` + snapshot; **programmatic get-by-id** is first-class (`GET /subgraphs/{id}` + domain `get(id)`) |
| G-S5  | Induce-on-save                  | deferred     | Not v1 |
| G-S6  | Delete cascade                  | **resolved** | Entity/edge delete → all packs; delete pack → membership only |
| G-S7  | Matcher + get-by-id             | **resolved** | **(1) Programmatic get-by-id:** domain + `GET /api/v1/objs/graph/subgraphs/{id}` (app-held `subgraphId` refs — no expression). **(2) `subg-expr`:** JEXL over pack header (`id`, `a.*` annotations), same engine spirit as `obj-expr`; matching packs → **union** of stored member entities + stored member edges (not re-induce); chainable as stage-0 source then object-level filters. **(3) Optional sugar** `subgraph: { id }` in matcher DSL OK (same as get-by-id) for query handoff. **UI (Explorer/Composer)** uses **`subg-expr`** (and list) to discover packs; open selected pack via get-by-id / replace |
| G-S8  | No name column                  | **resolved** | Annotations only on header |
| G-S9  | Gremlin flatten/nest            | deferred     | Not this story |
| G-S10 | SBOM demo                       | **resolved** | **No** demo seed in `:objs-sbom-example` |
| G-S11 | Annotation vocabulary           | **resolved** | Free-form app-level key/value; no platform vocab |
| G-S12 | Soft-link liveness              | **resolved** | Latest object/edge on resolve |
| G-S13 | Snapshot semantics              | **resolved** | Hard materialization for governance evidencing; new evidence-pack subgraph over clones; app-level immutability policy |
| G-S14 | Snapshot annotation apply       | **resolved** | Required `annotations` stamp **new header** and **cloned entities** (merge-overlay on clones) |
| G-S15 | Soft vs hard                    | **resolved** | Always create a subgraph; soft = live links; hard = clone then link |

## Cold-start notes for implementers

- Prefer failing closed on membership validation (missing entity/edge id, edge endpoints not members).
- Snapshot must run in **one transaction**: persist clones + create new subgraph membership, or neither.
- Snapshot should reject if source missing; empty subgraph snapshot may create empty clone subgraph (document in tests — prefer allow empty).
- Do not put TinkerPop / Gremlin dependencies into `:objs-core`.
- `subg-expr` bindings: at least `id` (string) and `a` (annotations map); mirror `obj-expr` sandbox / pushdown policy where practical (local eval acceptable for v1 if pack count is small).
