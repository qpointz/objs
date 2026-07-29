# WI-000 — Matcher DSL and abstraction

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 0 — DSL and abstraction  
**Status:** done  
**Depends on:** G-M1–G-M9 in [`GAPS.md`](GAPS.md)

## Goal

Define the concise, extensible matcher DSL and resolve both JSON and YAML into concrete
implementations hidden behind `BoMMatcher`. Model an ordered array as one chained/composite matcher
without adding pipeline-specific contracts to callers.

## Scope

- Define the canonical DSL:
  - one object = one matcher;
  - one array = one ordered matcher chain;
  - exactly one registered key per matcher object;
  - `anno` accepts a string-to-string annotation map;
  - `anno-expr` accepts a JEXL expression string.
- Introduce a matcher DSL codec/factory that:
  - accepts JSON and YAML;
  - produces `BoMMatcher`;
  - uses an extensible matcher-key handler registry rather than a closed controller switch;
  - reports stable path-aware errors for malformed and unknown matcher nodes.
- Add `BoMChainedMatcher` (working class name) as a `BoMMatcher` implementation with a non-empty,
  immutable ordered list of child matchers.
- Preserve `BoMMatcher.matchesEdge(...)` behavior; chained entity matching must not alter the
  induced-edge contract.
- Represent `anno` with the existing `MatchAllAnnotationMatcher` semantics rather than duplicating
  annotation equality behavior.
- Keep existing matcher constructors and store entry points source-compatible.
- Add canonical JSON/YAML examples and parser/serializer parity tests.

## Design constraints

- Controllers, services, graph stores, SDK consumers, and DSL callers operate on `BoMMatcher`.
- Concrete DSL node DTOs, format detection, handler lookup, and chain decomposition are internal
  construction/execution details.
- A single-element array may resolve to a chained matcher or be normalized to its only child, but
  observable selection semantics and serialization must remain deterministic.
- Reject empty arrays, empty matcher objects, multi-key matcher objects, unknown matcher keys, wrong
  value types, and an empty `anno` map.
- Serialization emits one canonical representation even though parsing accepts JSON and YAML.

## Out of scope

- JEXL engine integration (WI-001)
- Persistence execution planning and pushdown (WI-002)
- HTTP endpoints (WI-003)
- OR, nested groups, branching, or conditional stages

## Acceptance

- [x] JSON and YAML examples resolve to equivalent `BoMMatcher` behavior
- [x] `anno` delegates to existing match-all annotation semantics
- [x] A root array resolves to a `BoMMatcher` composite whose children retain declaration order
- [x] Consumers can parse and execute without referencing `BoMChainedMatcher` or DSL DTO classes
- [x] Matcher-key registration supports future matcher kinds without changing the root parser
- [x] Empty/unknown/multi-key/incorrectly typed nodes return stable path-aware validation errors
- [x] Existing matcher hierarchy and annotation matcher compatibility tests remain green
