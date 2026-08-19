# Story: Versions and snapshots

**Slug:** `versions-and-snapshots`  
**Branch:** `versions-and-snapshots`  
**Status:** closed  
**Folder:** [`docs/workitems/completed/20260819-versions-and-snapshots/`](.)  
**Backlog:** [C-18](../../BACKLOG.md) (done)  
**Depends on:** **C-17 complete** (live `copyGraph` + `mergeGraph`); C-12 `clone()` **kept** (deep copy, new ids). This story adds Snapshot = `createDeepGraphVersion`. **Next:** [C-19](../../planned/foundation-after-versions/STORY.md)  
**Design:** [`docs/design/graph/apps-vs-foundation.md`](../../../design/graph/apps-vs-foundation.md)  
**Target ER:** [`ER.md`](ER.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)  
**GitLab:** [sandbox/bom-poc#2](https://gitlab.qpointz.io/sandbox/bom-poc/-/work_items/2)

**Scope:** entity/edge/graph **HEAD + history**, audit clocks, **`createDeepGraphVersion`**. Not C-17 lookups. Not lock/visibility flags. **Not** a second snapshot graph id.

## Goal

Stable **identities**. Persist updates HEAD; **when** a version row is written is a **`BomVersioningStrategy`** (C-18 default: never on persist — today).  
**Live graphs** use HEAD tables only.  
**Deep graph version** (`createDeepGraphVersion`) always captures full graph state. Same `graph_id`. Reconstruct is the slower path.

Fingerprints do not add pool rows. Live catalog is identities at HEAD. Edges are versioned like entities. Full graph state is historized only on **deep** freeze (not on every node save).

## Live vs deep graph version

| | Live graph | Deep graph version |
|--|------------|---------------------|
| Identity | `graph_id` HEAD | `(graph_id, version)` BIGINT on that graph |
| Members / edges | HEAD rows | pins to entity/edge **versions** |
| Read | HEAD tables only | reconstruct from `*_version` (slower OK) |
| Edit | in-place HEAD (no version row) | none (pins do not move) |
| `copyGraph` / `mergeGraph` | new live graph, same entity ids | — (draft-from-fingerprint = copyGraph of **live** HEAD) |
| `clone()` | new live graph, **new** entity/edge ids (current HEAD only) | — (clone starts an **empty** history line) |
| Freeze / Snapshot | — | `createDeepGraphVersion` on the **same** `graph_id` (memorized milestone). Does **not** replace `clone()` |

## Stages

| Stage | WIs | Consumers | Notes |
|-------|-----|-----------|-------|
| 0 — Scaffold | WI-000 | — | This folder |
| 1 — Design lock | WI-001 | **docs** | Fold [`ER.md`](ER.md) into living design; cancel C-19 clocks |
| 2 — Audit clocks | WI-002 | workbench + SBOM + AR (JSON) | Flyway **V3**; `created_at` / `updated_at` on every `bom_*` that lacks them |
| 3 — Version store | WI-003 | — | Flyway **V4** tables; persist still in-place; `head_version` null |
| 4 — Deep graph version | WI-004 | — | Pin children + reconstruct; **keep** `clone()` |
| 5 — Workbench | WI-005 | **workbench** | Explorer version list (newest first) + Latest; Composer **Snapshot** (freeze) **and** **Clone** |
| 6 — SBOM | WI-006 | **SBOM** | Fingerprint = `(graph_id, graph_version)` |
| 7 — Docs | WI-007 | **docs** | Sweep + **[`docs/design/graph/database-model.md`](../../../design/graph/database-model.md)** (detailed as-built schema) |

## Examples (required)

Per [`RULES.md`](../../RULES.md) **Concrete example integration**, examples must stay **functional** through this story — not a follow-up story.

| App | Must still work | C-18 change |
|-----|-----------------|-------------|
| **SBOM** (`:sbom-service` + UI) | Inventory, BOM drafts, Combined GET, keep-split `copyGraph`, combine `mergeGraph`, demo seed | Fingerprint freeze = `createDeepGraphVersion`; open fingerprint = reconstruct; clocks on JSON if exposed |
| **AR** (`:asset-repository-service` + UI) | Collections, objects, collection **copy** (`copyGraph`), demo | Live HEAD only; **no** freeze product; persist/copy/tests still green |

After every WI that touches `bom_*` or persist (WI-002, WI-003, WI-004, WI-006): `./gradlew :sbom-service:test :asset-repository-service:test` (plus UI tests if that WI changes SPA). WI-007 does not close until those modules still pass.

AR: live collections stay HEAD; **no AR freeze product** in this story.

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock (`WI-001-design-lock.md`)
- [x] WI-002 — Audit clocks (`WI-002-audit-columns.md`)
- [x] WI-003 — Version store (`WI-003-version-store.md`)
- [x] WI-004 — Deep graph version (`WI-004-deep-graph-version.md`)
- [x] WI-005 — Workbench (`WI-005-workbench.md`)
- [x] WI-006 — SBOM fingerprint (`WI-006-sbom.md`)
- [x] WI-007 — Living docs (`WI-007-living-docs.md`)

## Out of scope

- Implementation of later WIs before their predecessor is `[x]` (WI-001 is this lock)
- C-17 catalog helpers, reverse lookup, identity query, `copyGraph` / `mergeGraph`, paging (already shipped)
- C-20 text `q` / contains
- `writeHold` / `catalogScope` / annotation isolation
- AR collection freeze product
- Per-keystroke versions; version GC; product semver
- Concrete `OnWrite` / per-graph / union-of-member strategies (SPI only in C-18)
- AuthZ / `created_by`
- Point-in-time membership without `createDeepGraphVersion`
- Postgres HEAD→version **triggers** (optional later; H2 will not have them)
- Backward compatibility; data migration; history backfill (greenfield: recreate DB)
- Copying source `*_version` rows onto a clone (clone copies HEAD only)

## Acceptance (when implemented)

- Live GET does not join `*_version`; GIN stays on HEAD tables
- Ordinary persist does not write `*_version` (default `ExplicitOnly` strategy)
- Store asks `BomVersioningStrategy`; C-18 ships that default only
- `createDeepGraphVersion` copies current HEAD into version rows, then pins; later in-place edits do not change that freeze
- Delete HEAD keeps `*_version`; deep reconstruct still works
- Live edit after a deep freeze does not change that freeze
- Deep freeze does not create extra pool identities; pins keep provenance to original entity/edge ids + version
- Edges are versioned; a deep version historizes header + members + edges
- SBOM fingerprint reconstructs pin-time payloads from `(graph_id, graph_version)`
- `copyGraph` / `mergeGraph` still share identities at HEAD
- `clone()` still new ids from current HEAD; clone has no `*_version` rows until its own Snapshot
- Snapshot / freeze does not create a new graph; `POST /graphs/{id}/clone` remains
- [`docs/design/graph/database-model.md`](../../../design/graph/database-model.md) exists and matches shipped Flyway/JPA
- `:sbom-service` and `:asset-repository-service` tests pass; SBOM fingerprint and AR collection copy still work end-to-end

## Process notes

1. One WI at a time during implementation; history rewritten at close into logical review commits.  
2. Closed 2026-08-19 (UTC).
