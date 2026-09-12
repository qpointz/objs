# Story: Transaction + Spring integration recipes

**Slug:** `transaction-recipes`  
**Branch:** `transaction-recipes`  
**Status:** in-progress  
**Folder:** [`docs/workitems/in-progress/transaction-recipes/`](.)  
**Backlog:** [C-37](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Depends on:** C-25 (`objs-core-spring-split`)  
**Design:** [`spring-integration.md`](../../../design/core/spring-integration.md) · [`transaction-recipes.md`](../../../design/core/transaction-recipes.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Docs-only: document how Boot apps embed objs via `:objs-autoconfigure`, and how transactions work in Spring (`@Transactional` join) vs non-Spring (`EntityManagerUnitOfWork`) contexts. Extend existing recipe indexes with pointers. No persistence rewrite; no `objs-persistence-spring`.

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Spring integration + transaction recipes docs (`WI-001-living-docs.md`)

## Out of scope

- Harden `TransactionTemplateUnitOfWork` join short-circuit
- Integration tests proving app `JpaRepository` + graph mutate roll back together
- Dual Spring Data persistence module

## Acceptance

- [x] `docs/design/core/spring-integration.md` and `transaction-recipes.md` exist
- [x] Cross-links from programmatic-recipes, core/graph READMEs, persist-sketch, spring-split, design README
- [x] Outdated “callers do not open TX” wording corrected where it conflicts with join recipe
