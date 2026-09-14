# Gaps — api-store-improvements (C-38)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

| Area | Close in |
|------|----------|
| Exists `id` (header-only) | G-E1 **resolved**; code WI-001 |
| Exists `matcher` (any + early exit) | G-E2 **resolved**; kinds G-E5; code WI-001 |
| REST exists | G-E3 **deferred** |
| `createDeepGraphVersion` default | G-E4 / WI-006 (**done**) |
| Graph context Open overlap (Note1) | G-U1 → WI-007; G-U2 views locked |
| ValidationIssue shape | G-V1…G-V7 **resolved** (WI-002 lock); code WI-003+ |
| Living docs | G-D1 **resolved**; write-up WI-005 |

---

## Exists (WI-001)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-E1 | `exists(UUID)` | **resolved** | **Header-only** — `graphDao.existsById` (or equiv.); no membership/edge/`ResolvedGraph` load. Impl WI-001 |
| G-E2 | `exists(Matcher)` | **resolved** | **Any** matching graph; **early exit** on first hit. Header path only — no entity/edge/`ResolvedGraph` load. See G-E5 for which matcher kinds. Impl WI-001 |
| G-E5 | Matcher kinds for `exists` | **resolved** | **`GraphExprMatcher`:** header search/pushdown, stop at first hit. **`GraphIdsMatcher`:** OR over listed ids via header exists; **early exit**; **empty ids → `false`** (not an error). **`AllGraphsMatcher`:** true iff ≥1 graph header exists (cheap any-row / `EXISTS`, not full list). **`ChainedMatcher` / `ObjExprMatcher` / other entity-scoped:** **fail fast** (`IllegalArgumentException` or `GraphException`) — not silent `false`. |
| G-E3 | REST exists | **deferred** | No REST / HEAD in this story |
| G-E4 | `createDeepGraphVersion` default | **resolved** (WI-006) | No-`at` overload calls `Instant.now()` at invoke time; backdated overload takes non-null `Instant` |

---

## Workbench chrome (WI-007)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-U1 | Graph selector overlaps Open | **resolved** (WI-007) | Trailing Open+N/E `flexShrink: 0`; metadata `flex:1; minWidth:0; overflow:hidden`. Same for Composer. |
| G-U2 | Views in scope | **resolved** (validate) | **Affected:** Explorer, Objects, Query, Policy (`GraphContextBar`); Composer (`ComposerGraphBar`). **N/A:** Schema (no header graph Open) |

---

## ValidationIssue (WI-002 lock → WI-003/004 code)

All G-V* **resolved** 2026-09-14. WI-003 implements; WI-004 consumes in workbench.

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-V1 | Subject shape | **resolved** | `ValidationSubject`: `kind` (ENTITY\|EDGE\|GRAPH\|OTHER), `id`, `index`, `type`, `schemaVersion`, **`document`** (payload or edge properties), edge extras `role` / `sourceId` / `targetId`. **`index` always** when validating a `*.set` / list item; **`document` when that document was validated**, else null. Redundancy OK; no path parse. |
| G-V2 | Schema / locus shape | **resolved** | `ValidationSchemaRef`: `locus` (ENTITY_PAYLOAD\|EDGE_PROPERTIES\|EDGE_ALLOWLIST\|IDENTITY\|MEMBERSHIP\|OTHER), `schemaType`/`schemaVersion`, `fieldPath`, `allowedEdge` (`sourceType`,`role`,`targetType`) when EDGE_ALLOWLIST. |
| G-V3 | Emit sites | **resolved** | Graph persist-gate (`Validator` + store membership / identity) **always** populate `subject`+`schema` when about an entity/edge/rule. Seeds/registry may leave null in v1 (**deferred retrofit**). |
| G-V4 | networknt fieldPath | **resolved** | `fieldPath` = **JSON Pointer** (`/name`, `/weight`) from networknt instance location. One `SCHEMA_VIOLATION` issue per networknt error. |
| G-V5 | Keep `path` | **resolved** | Optional debug string; clients **must not** require parsing it. WI-004 uses `subject.*`. |
| G-V6 | Embed full Entity/Edge? | **resolved** | **No** full domain object — `document` map + ids/index only. |
| G-V7 | Multi-field IDENTIFIER_IMMUTABLE | **resolved** | **One** issue; `fieldPath` **null**; changed paths remain listed in `message` (current behaviour). |

---

## Living docs (WI-005)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-D1 | Doc updates | **resolved** (WI-005) | Updated `persist-sketch.md` + `validation.md`; ui.md unchanged (WI-007 layout-only) |

---

## Decision log

| # | Decision | Date | Summary |
|---|----------|------|---------|
| PARK | Story parked | 2026-09-14 | C-38 API store improvements; branch `api-store-improvements` |
| G-E4 | Default freeze overload | 2026-09-14 | No-`at` → `Instant.now()`; backdated takes non-null `Instant` (WI-006) |
| G-E1 | exists(id) header-only | 2026-09-14 | Header existence only; never full `get` |
| G-E2 | exists(matcher) any + early exit | 2026-09-14 | First matching graph wins; no full content load |
| G-E5 | exists matcher kinds | 2026-09-14 | GraphExpr / GraphIds / AllGraphs only; empty GraphIds → false; others fail fast |
| G-U2 | Note1 view matrix | 2026-09-14 | Explorer/Objects/Query/Policy/Composer; Schema out of scope |
| G-U1 | Open layout fix | 2026-09-14 | Metadata shrinks left; Open+N/E pinned (WI-007) |
| G-V6 | No full Entity/Edge embed | 2026-09-14 | `document` + address fields only |
| G-V1…V5 | ValidationIssue lock | 2026-09-14 | Subject+schema addressable; JSON Pointer fieldPath; seeds null OK v1; path debug-only |
| G-V7 | Identity multi-field | 2026-09-14 | Single issue; fieldPath null; paths in message |
| G-D1 | Living docs | 2026-09-14 | Update persist-sketch (+ validation notes); ui.md only if WI-007 chrome copy changes |
