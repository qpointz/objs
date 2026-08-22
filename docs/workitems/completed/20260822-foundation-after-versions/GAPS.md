# Gaps — foundation-after-versions (C-19)

Depends on C-18 pins/HEAD (**shipped**). C-19 implements G-A3 and G-A4.

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-A1 | Entity/edge clocks | **resolved** | C-18 — `createdAt` on version row; identity `updatedAt` = last HEAD move |
| G-A2 | Graph clocks | **resolved** | C-18 — header `createdAt`/`updatedAt`; snapshot graph `createdAt` = pin time |
| G-A3 | Reverse lookup | **resolved** | [`pin-reverse-lookup.md`](../../../design/graph/pin-reverse-lookup.md) — union live membership + pin graphs in `listGraphIdsForEntity` |
| G-A4 | FB-3 remainder | **resolved** | [`matcher-pushdown-remainder.md`](../../../design/graph/matcher-pushdown-remainder.md) — `>`, prefix; contains/`q` = C-20 |
| G-X1 | C-17 in-place entity timestamps | **cancelled** | Do not do C-17 WI-009 entity/edge Flyway |
