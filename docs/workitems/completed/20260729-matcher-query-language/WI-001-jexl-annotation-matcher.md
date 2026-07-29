# WI-001 — JEXL annotation expression matcher

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Annotation expression  
**Status:** done  
**Depends on:** WI-000; G-J1–G-J6 in [`GAPS.md`](GAPS.md)

## Goal

Implement `anno-expr` with Apache Commons JEXL as a sandboxed, bounded,
`BoMNonPushableMatcher` that evaluates predicates against entity annotations only.

## Scope

- Add Apache Commons JEXL through the Gradle version catalog and `objs-core`.
- Implement the JEXL matcher behind `BoMMatcher`; do not add raw script text to
  `BoMMatchExpression`.
- Build one immutable, thread-safe, application-scoped JEXL engine shared by matcher instances.
- Compile and validate with `createExpression` when the matcher is constructed, not once per
  candidate; use a bounded shared compilation cache.
- For each candidate, create a fresh read-only context backed by
  `BoMEntityMatchCandidate.annotations`.
- Bind each annotation key as a direct expression variable, for example:
  `version == '1.0.0' && app == 'aapp-lala'`.
- Configure the engine according to resolved G-J4/G-J5 decisions:
  - allow only variable reads, literals, parentheses, Boolean logic, null checks, and comparisons;
  - deny scripts, assignment/side effects, properties/indexing, constructors, reflection/class
    access, object creation, mutation, namespaces/functions, methods, loops, lambdas, local
    variables, pragmas, and JEXL annotations;
  - keep a simple shared compile cache (suggested capacity 256);
  - reject oversized expression text (suggested max 4 KiB);
  - enforce one hard 3-minute wall-clock budget for the whole selection request;
  - do not implement cooperative cancellation or per-expression interrupt machinery;
  - use deterministic syntax, safety, and evaluation error reporting.
- Ensure matcher instances and any shared compiled-expression cache are safe for concurrent reads.
- Add focused tests for equality, Boolean operators, missing keys/nulls, invalid syntax, prohibited
  operations, resource limits, and concurrent evaluation.

## Semantics

- The expression result must be Boolean; other result types fail validation/evaluation according to
  the resolved error contract.
- Per-candidate runtime errors fail the selection request; they are not silently converted to
  `false` unless G-J6 is changed before implementation.
- JEXL evaluation is always in memory, including when `anno-expr` is the first or only matcher.
- No map object, object payload, entity scalar field, or graph relationship is added to the
  expression context.
- Annotation keys referenced by `anno-expr` must be valid JEXL identifiers. The `anno` matcher
  remains available for arbitrary string keys.

## Out of scope

- Payload/content predicates
- Entity id/type/schema-version bindings
- Edge or neighbor predicates
- JEXL-to-SQL compilation
- User-provided functions or namespaces

## Acceptance

- [x] `anno-expr` resolves to a `BoMNonPushableMatcher`
- [x] A shared immutable engine evaluates direct annotation variables with safe
      Boolean/comparison operations
- [x] Payload, entity internals, classes, constructors, reflection, mutation, and arbitrary methods
      are inaccessible
- [x] Scripts, assignment, properties/indexing, functions, loops, lambdas, and object creation are
      rejected
- [x] Expressions compile once per matcher/cache entry rather than once per entity
- [x] Invalid, unsafe, non-Boolean, oversized, and over-budget (3-minute) selections fail with
      stable errors
- [x] Missing annotation keys have documented and tested null semantics
- [x] Concurrent matcher evaluation is deterministic and thread-safe
