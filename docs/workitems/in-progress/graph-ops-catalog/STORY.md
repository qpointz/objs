# Story: graph-ops-catalog — staged graph lifecycle ops

**Slug:** `graph-ops-catalog`  
**Branch:** `graph-ops-catalog`  
**Status:** in-progress  
**Folder:** [`docs/workitems/in-progress/graph-ops-catalog/`](.)  
**Backlog:** [C-36](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Before:** [C-18](../../completed/20260819-versions-and-snapshots/STORY.md), [C-22](../../completed/20260826-graph-mutate-replace/STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md) — **all design rows locked** (A–C resolved; G-O16a/b → Stage D)  
**Docs:** [`persist-sketch.md`](../../../design/graph/persist-sketch.md) · [`programmatic-recipes.md`](../../../design/graph/programmatic-recipes.md) (WI-005) · [`rest-api.md`](../../../design/service/rest-api.md)  
**Process:** [`RULES.md`](../../RULES.md)

## Goal

Deliver a **clear / purge / destroy** lifecycle (plus softish **delete**), **compact***, backdated freeze, REST + Composer, recipes, and example seeds — in **stages**. Stage D adds travel-back **reset** and membership-only **apply**.

## Locked ops (summary)

| Op | Role |
|----|------|
| `clearGraph` | Empty HEAD members+edges; history kept (`≡` REPLACE-empty) |
| `purgeGraphVersion` / `purgeAllGraphVersions` | Drop freeze(s); reject purge of current `head_version`; erase-all nulls `head_version`, HEAD content intact |
| `delete` | Softish: drop live graph, **keep** history |
| `destroyGraph` | Full wipe HEAD+history; fail if SBOM fingerprints reference versions |
| `compactEntity` / `compactEdge` | Explicit orphan instance-version GC |
| `createDeepGraphVersion(…, at?)` | Option B: version+clocks from `at` on new rows; reuse keeps existing clocks |
| `GraphOperationException` | Extends existing `ObjsException`; migrate `GraphException` |
| `resetGraphToVersion` | **Stage D** — travel back (membership + payloads) |
| `applyGraphVersionMembership` | **Stage D** — membership from freeze, **latest** payloads |

## Stages

```text
0 Scaffold ──► 1 Design lock ──► A Store lifecycle ──► B Backdated freeze
                                      │                      │
                                      └──────────┬───────────┘
                                                 ▼
                                    C1 REST + Composer (A–C ops)
                                                 ▼
                                    C2 Recipes doc
                                                 ▼
                                    C3 Living docs + SBOM/AR backdated seeds
                                                 ▼
                                    D Reset + Apply   (explicit ask)
```

| Stage | Ready when | WIs | Deliverable |
|-------|------------|-----|-------------|
| **0** Scaffold | — | WI-000 | **done** |
| **1** Design lock | WI-000 done | WI-001 | GAPS locked; this staged STORY |
| **A** Store lifecycle | WI-001 done | WI-002 | clear/purge/destroy/compact + exception hierarchy |
| **B** Backdated freeze | WI-001 done (∥ A) | WI-003 | `createDeepGraphVersion(…, at)` Option B |
| **C1** REST + Composer | A+B done | WI-004 | HTTP + Composer for A–C ops (not D); tour |
| **C2** Recipes | C1 done | WI-005 | `programmatic-recipes.md` |
| **C3** Docs + examples | C2 done | WI-006 | Living glossary; SBOM/AR backdated version seeds |
| **D** Reset + Apply | C3 done + **user asks** | WI-007 | G-O16a travel-back + G-O16b version-apply |

**Parallelism:** A and B may run sequentially on one branch (preferred: A then B) or B right after A; both before C1.  
**Stop after C3** is a valid ship point; Stage D is optional continuation on the same branch/story.

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock + staged plan (`WI-001-design-lock.md`)
- [x] WI-002 — Store: clear/purge/destroy/compact + `GraphOperationException` (`WI-002-store-lifecycle.md`)
- [x] WI-003 — Backdated freeze (`WI-003-backdated-freeze.md`)
- [x] WI-004 — REST + Composer for Stages A–C (`WI-004-rest-composer.md`)
- [x] WI-005 — Programmatic recipes (`WI-005-programmatic-recipes.md`)
- [x] WI-006 — Living docs + example backdated versions (`WI-006-docs-examples.md`)
- [x] WI-007 — Stage D: reset + apply (`WI-007-reset-to-version.md`)

## Out of scope

- Global batch GC beyond `compact*`
- AuthZ / recycle bin
- Merging recipes into `persist-sketch.md`

## Acceptance

### After Stage C3 (A–C complete)

- [x] Ops match GAPS; tests green (`:objs-persistence`, `:objs-service`, examples touched)
- [x] REST + Composer for clear/purge/destroy/delete/compact/dated freeze; tour OK
- [x] Recipes + sketches cross-linked
- [x] Examples show multiple backdated versions
- [x] G-O16 remains Stage D only

### After Stage D

- [x] Travel-back reset (+ optional truncate) and membership-only apply; separate REST/UI
- [x] Shared-pool rewrite documented for travel-back only

## Process

1. One WI → `[x]` → one commit → push.  
2. WI-002 only after WI-001. WI-007 only after WI-006 **and** explicit Stage D ask.  
3. Story closure only when the user asks.
