# Gaps — store-text-search (C-20)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

**WI-001 must close every `open` row** (resolve or defer) before store code. Draft guesses below are **not** locks.

---

## Open (design)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-A1 | API shape | **open** | Separate `q: String?` on `selectFromPool` / `selectInGraph` vs `obj-expr` contains (`p['name'] ~= 'foo'` / `contains`)? One DSL only |
| G-A2 | Field set | **open** | `type` + identifier scalars + searchable scalars (C-17 G-A19 draft)? Searchable-only? Nested OBJECT vs first-level only? ARRAY out? |
| G-A3 | Match semantics | **open** | Case-insensitive substring? Multi-word AND vs one haystack? Trim / empty `q` = no extra predicate? |
| G-A4 | SQL strategy | **open** | Query-only JSON `ILIKE` / H2 `LOWER LIKE` vs generated column / index. Vendor SQL in objs Flyway only if indexing is required |
| G-A5 | Scope | **open** | Pool (orphans included) and graph-scoped both required? Combine with type matcher + C-17 paging |
| G-A6 | Slow path | **open** | If a type’s schema has no searchable/identifier scalars, match `type` only or skip? Document |
| G-A7 | Live vs snapshot | **open** | This story searches **live** members. Pin-aware search after C-18 is out unless WI-001 explicitly pulls it in |

---

## Provisional (likely keep)

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-A8 | Domain name search | **resolved** | SBOM application `LIKE` and AR collection `LIKE` stay in the example |
| G-A9 | Graph header search | **resolved** | Workbench `GET /graphs/search?q=` stays header substring; not payload `q` |
| G-A10 | Layer | **resolved** | `:objs-core`; examples do not call workbench REST as the app data API |
| G-A11 | Consumers | **resolved** | Implementation WI rewires workbench Objects + SBOM asset list + AR collection object search together |

---

## Out of story

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-X1 | `tsvector` / ranking / stemming | **deferred** | Later FTS story |
| G-X2 | FB-3 operators other than contains/`q` | **deferred** | **C-19** [`foundation-after-versions`](../foundation-after-versions/STORY.md) |
| G-X3 | Schema-explorer client `includes` | **cancelled** | Small catalog; leave client-side |
| G-X4 | C-17 paging / catalog / copy / merge | **deferred** | [`live-store-apis`](../../completed/20260819-live-store-apis/STORY.md) |
