# WI-002 — Generate / implement materializer

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Runtime / generator  
**Status:** planned  
**Depends on:** WI-001  
**Examples:** **—**

## Goal

Implement the locked aggregate materializer in `:objs-codegen-java` (generated facade) and any schema-agnostic helper approved in WI-001.

## Scope

- Walk nested POJOs per locked relation metadata
- Emit `GraphMutation` (and/or builder registration) with entities + edges
- Strip / ignore relation collection fields on parent payloads
- Diagnostics + fail-closed for locked error cases
- Unit tests on a multi-type fixture ontology
- No store persist changes

## Out of scope

- Example module smoke (WI-003)
- Living docs polish (WI-004)

## Acceptance

- [ ] Nested Product→Component (or fixture) materializes to expected entity/edge counts
- [ ] Parent payload map has no embedded child entity objects
- [ ] `./gradlew :objs-codegen-java:test`
