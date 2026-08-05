# WI-010 — Anno-expr as source-capable pushdown

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3b — Anno-expr source (follow-on to WI-003)  
**Status:** done  
**Depends on:** WI-002, WI-003

## Goal

Make DSL `anno-expr` / `BoMAnnoExprMatcher` **source-capable** when the JEXL AST lowers to the same equality∧`&&` shape as match-all, reusing Postgres JSONB `@>` containment.

## Scope

- Walk compiled JEXL AST (`BoMAnnoExprLowerer` / `ScriptVisitor`).
- Lower identifier `==` / `===` string-literal (+ parentheses unwrap + `&&`) to `BoMMatchExpression`.
- `BoMAnnoExprMatcher` implements `BoMSourceCapableMatcher` → `annotationContainmentSource`.
- Unsupported shapes (`||`, `!=`, null checks, number literals, …) return null → AllEntities + `matches`.
- DSL key/text unchanged.

## Out of scope

- Richer SQL for OR / inequality / comparisons
- JSONB/GIN (WI-007)
- Lazy JSON (WI-006)

## Acceptance

- [x] Lowerable first-stage `anno-expr` on Postgres uses containment source
- [x] Non-lowerable expressions remain filter-only with identical `matches` semantics
- [x] Unit tests for lowerer + source-capable / null-source cases
