# Gaps — policy-seeds-persistence (C-28)

Close in this story’s WI-001 only.

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P13p | JPA / Flyway backing store | **open** | Tables on objs Flyway line vs policy submodule history |
| G-P14p | Persistent repository API | **open** | Pagination? optimistic locking? suite CRUD if C-27 |
| G-P34seed | Seed envelope | **open** | Same `apiVersion: objs.poc.org/v1`? |
| G-P35seed | Seed kinds | **open** | `Policy`, `PolicySuite` nesting |
| G-P36seed | MERGE keys | **open** | Policy logical key by **name**; each apply creates/updates a **serial version** (C-24 G-P3) — not free-form overwrite of version string |
| G-P37seed | Body embedding | **open** | Inline vs file ref |
| G-P38seed | Import path | **open** | `SeedDocumentHandler` vs dedicated importer |
| G-P39seed | Apply order | **open** | Policies before suites; vs ObjectSchema/Graph |
| G-P40seed | Validation | **open** | Unknown engineKind; dangling refs; suite cycles |

## Philosophy (inherited)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P36s | Seed format required | **resolved** (intent) | Content in apps |
| G-P41 | Dedicated store | **resolved** | Not `bom_entity` |

## Decision log

| # | Decision | Date | Summary |
|---|----------|------|---------|
| — | — | — | — |
