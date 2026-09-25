# WI-003 — Consumer proof

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Consumers  
**Status:** planned  
**Depends on:** WI-002  
**Examples:** **codegen (+ locked apps)** — [`EXAMPLES.md`](EXAMPLES.md)

## Goal

Regenerate consumer bindings and add smoke tests proving nested POJO → mutation without hand-wired relation methods for the happy path.

## Scope

- Locked example module(s) green after regenerate
- Test compares materialize output to equivalent explicit `GraphMutationBuilder` assembly (or asserts structure)
- Optional SBOM/AR only if WI-001 locked them

## Out of scope

- Domain REST changes
- Migrating all existing write call sites

## Acceptance

- [ ] Locked example tests pass
- [ ] `./gradlew` for those modules + `:objs-codegen-java:test`
