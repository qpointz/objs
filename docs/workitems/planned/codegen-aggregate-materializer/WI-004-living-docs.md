# WI-004 — Living docs

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Docs  
**Status:** planned  
**Depends on:** WI-003  
**Examples:** **docs**

## Goal

Document explicit aggregate materialization as the supported path from nested jsonschema2pojo graphs to `GraphMutation`, and keep “relations are not payload nests” crisp.

## Scope

- [ ] [`docs/design/graph/codegen-and-builder.md`](../../../design/graph/codegen-and-builder.md) — move materializer out of “Deferred”; API + strip rules
- [ ] [`docs/design/graph/api-and-codegen.md`](../../../design/graph/api-and-codegen.md) — write model section
- [ ] Recipe: nest → materialize → validateMutate / mutate (link persist-sketch / programmatic-recipes)
- [ ] Cross-link C-43 catalog when used
- [ ] Point sibling follow-ups to BACKLOG C-45…C-47

## Acceptance

- [ ] Deferred list no longer lists recursive aggregate materialization as undone without pointer to this story
- [ ] Explicit-only warning is impossible to miss
