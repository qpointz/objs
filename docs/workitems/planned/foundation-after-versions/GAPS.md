# Gaps — foundation-after-versions (C-19)

Depends on C-18 pins/HEAD. **No work until C-18 has shipped the version store.**

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-A1 | Entity/edge clocks | **resolved** | `createdAt` on **version** row; identity `updatedAt` = last HEAD move. Not in-place `bom_entity` columns from C-17 |
| G-A2 | Graph clocks | **resolved** | Header `createdAt`/`updatedAt` here (or with WI-002). Snapshot graph `createdAt` = pin time |
| G-A3 | Reverse lookup | **resolved** | Extend C-17 membership lookup with graphs that **pin** a version of the identity |
| G-A4 | FB-3 | **resolved** | contains/`q` is **C-20**. Here: remaining operators worth pushing down |
| G-X1 | C-17 in-place entity timestamps | **cancelled** | Do not do C-17 WI-009 entity/edge Flyway |
