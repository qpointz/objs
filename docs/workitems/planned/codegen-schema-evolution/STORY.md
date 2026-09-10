# Story: codegen-schema-evolution — typed multi-version deserialize + migrations

**Slug:** `codegen-schema-evolution`  
**Branch:** (not started — **planned** only; design first)  
**Status:** planned  
**Folder:** [`docs/workitems/planned/codegen-schema-evolution/`](.)  
**Backlog:** [C-35](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Before:** [C-23 `objs-api-codegen`](../../completed/20260828-objs-api-codegen/STORY.md)  
**Independent of:** policy family (C-29…C-34), [C-20 `store-text-search`](../store-text-search/STORY.md)  
**Design:** [`DESIGN.md`](DESIGN.md) (normative draft until WI-001)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Related living docs (promote after lock):** [`api-and-codegen.md`](../../../design/graph/api-and-codegen.md), [`codegen-and-builder.md`](../../../design/graph/codegen-and-builder.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)  
**Closes deferred:** C-23 [G-30](../../completed/20260828-objs-api-codegen/GAPS.md) (evolved-snapshot fixture) when implementation lands

## Goal

Objs already allows multiple entity/edge **schema versions** to coexist in the catalog and in persistence. Application codegen today emits a **latest-only** object model. This story designs (then later ships) a way to:

- keep a **minimal consumer object model** (generated, latest-centric);
- **deserialize** stored payloads whose `schemaVersion` is older than that model;
- do so via **typed, hand-maintained migrations** (not Map→Map authoring);
- keep a hard **separation** between overwrite-safe codegen artefacts and never-generated migration code;
- remain **transparent** to normal object-model readers (`ProductReadNode.payload()` → latest `Product`).

**This park is design-first.** No product/codegen implementation until WI-001 closes open GAPS and the user asks to start implementation WIs.

## Why now

- Persistence is pin-and-keep: `Entity(type, schemaVersion, payload: Map)` validated at write against that exact catalog row; reads do not upgrade.
- Typed hydrate is exact `(type, schemaVersion)` or raw fallback ([C-23 G-18](../../completed/20260828-objs-api-codegen/GAPS.md)).
- Mixed-version graphs therefore yield a partially typed view; consumers cannot assume “everything is latest `Product`.”
- Generating one full object-model family per historical version balloons surface area and still does not encode **semantic** upgrades (renames, splits, defaults).

## Normative (provisional — lock in WI-001)

See [`DESIGN.md`](DESIGN.md). Short form:

| Topic | Draft |
|-------|--------|
| Consumer API | Latest-only object model (Lane A codegen) |
| Migration authoring | Typed `From → To` steps; **reject** app-facing Map→Map |
| Historical DTOs | Lane B schema-snapshot classes (migration I/O only) — generation scope is a GAP |
| Migration bodies | Hand-maintained package; never under generated source sets |
| Persist on read | Store pin/payload unchanged (read/view upgrade) |
| Serialize any version | Raw/`Entity` + catalog; typed write stays latest |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | this park | Folder + BACKLOG/SEQUENCE/MILESTONE |
| 1 — Design lock | WI-001 | after WI-000 | Close open GAPS; promote DESIGN into living docs |
| 2 — Runtime SPI | WI-002 | after WI-001 + explicit ask | `objs-api` upgrade SPI + hydrate policy |
| 3 — Codegen + fixture | WI-003 | after WI-002 | Lane B export/generator hooks + G-30 example |
| 4 — Docs | WI-004 | after WI-003 | Living design; archive deferred rewrite if any |

## Work Items

- [ ] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [ ] WI-001 — Design lock: close GAPS; promote DESIGN (`WI-001-design-lock.md`)
- [ ] WI-002 — Runtime SPI + typed hydrate path (`WI-002-runtime-spi.md`) — **not started; design only until asked**
- [ ] WI-003 — Lane B codegen hooks + evolved-snapshot fixture (`WI-003-codegen-fixture.md`) — **not started**
- [ ] WI-004 — Living docs + G-30 close-out (`WI-004-living-docs.md`) — **not started**

## Out of scope

- Implementation of WI-002+ until the user starts this story **and** WI-001 is done
- Per-version object-model APIs (`ProductNode` / `productsV1()` per historical pin)
- Putting migration **bodies** inside codegen output
- App-facing Map→Map migration SPI
- Auto-rewrite-on-mutate of stored pins
- Graph HEAD / snapshot versioning (orthogonal)
- Aggregate materializer (C-23 G-19), generated HTTP client (G-20)

## Acceptance (after implementation — not this park)

- [ ] Stored older pins hydrate to latest OM types when a complete typed migration chain exists
- [ ] Missing chain → raw fallback (or locked fail-closed) without data loss of the underlying `Entity`
- [ ] Regenerating Lane A/B does not delete or overwrite hand migration sources
- [ ] G-30-style evolved-snapshot consumer fixture passes
- [ ] Open GAPS from WI-001 are resolved or explicitly deferred

## Process notes

1. One WI at a time; `[x]` + one commit + push per WI when implementing.  
2. Do not start WI-002 until WI-001 checkboxes are done.  
3. Do not close this story until the user asks.
