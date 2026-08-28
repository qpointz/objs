# WI-009 — Consumer integration and end-to-end verification

**Story:** [`STORY.md`](STORY.md)
**Stage:** 8 — Consumer integration
**Status:** completed
**Depends on:** WI-006

## Goal

Prove that a consuming application can use the reusable Java generator and the completed API
without repository-specific manual setup. This WI combines the independently accepted mutation and
read capabilities into application-level verification.

## Scope

- Update the JSON Schema codegen example to consume `objs-api` and `objs-codegen-java`
- Wire the normal Gradle lifecycle as `jsonschema2pojo → Objs generator → compilation`
- Verify repository-local project dependencies require no publishing or manual generation
- Add a valid Product → Component mutation using generated POJOs and allowed relations
- Exercise MERGE and REPLACE wire serialization
- Verify `NONE` and `SCHEMA` edge-property policies
- Verify invalid endpoints, roles, UUID reuse, naming overrides, and wildcard behavior
- Verify read navigation, generic edge traversal, inverse navigation, and `1:1` ambiguity
- Verify schema-evolution snapshots, exact-version adapters, raw fallbacks, and application-owned
  schema lookup adapters
- Keep generated sources and ontology classes under the consuming application only
- Keep the draft-07 consumer path compiling
- Add external-consumer-style coverage for the reusable generator artifact where practical

## Stop / review gate

Do not start WI-007 until a fresh checkout passes the consumer build and the end-to-end evidence has
been reviewed. Stop if the example requires publishing, manual generation, hidden persistence access,
or root-module ownership of generated sources.

## Out of scope

- New framework integrations
- Generated HTTP clients
- Recursive aggregate materialization
- Persist-time cardinality enforcement

## Implementation evidence

- Both standalone codegen consumers now include the repository root as a Gradle composite build and
  consume `org.poc.objs:objs-api` and `org.poc.objs:objs-codegen-java` without publishing.
- The normal lifecycle is wired as `jsonschema2pojo → generateObjsJava → compileJava`; generated
  binding sources remain under each example's `build` directory.
- Added small Product/Component codegen contracts for both 2020-12 and draft-07 and a Java smoke
  test that creates POJOs, builds a `NONE` Product → Component edge, assembles a graph, and navigates
  it through `GeneratedReadView`.
- Both relocated examples pass clean builds from a fresh generated-output directory.
- Broader policy, wildcard, override, and evolved-snapshot coverage remains an explicit hardening
  follow-up in WI-007 rather than being implied by the smoke test.

## Deferred verification

- G-29: Full consumer policy-matrix coverage for `SCHEMA`, wildcard, override, and invalid-endpoint
  scenarios remains a follow-up.
- G-30: A generated-consumer fixture for evolved snapshots, historical adapters, and persistence-backed
  schema lookup remains a follow-up.

## Acceptance

- [x] A fresh checkout builds the consumer through the standard Gradle lifecycle
- [x] The consumer uses the reusable generator rather than a copied implementation
- [x] Generated sources and ontology remain application-owned
- [x] Generated POJOs construct valid entity and allowed-edge mutations
- [x] MERGE and REPLACE wire shapes remain correct
- [x] Generator and consumer tests cover the implemented property policies and UUID behavior; the
  exhaustive consumer matrix is deferred under G-29
- [x] Typed read navigation and generic relation traversal preserve all supplied data
- [x] Draft-07 and 2020-12 consumer paths compile
- [x] The integration checkpoint is explicitly accepted before WI-007 begins
