# WI-003 — Graph-codegen JSON Schema and relation manifest

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Codegen export contract
**Status:** completed
**Depends on:** WI-008

## Goal

Extend the generic registry codegen export so standard JSON Schema drives application payload
classes while a deterministic Objs manifest describes the semantic relation operations needed by a
reusable generator running in the consuming application.

## Scope

- Add a graph-codegen export profile without changing the existing standard catalog export
- Keep `ENTITY` and `EDGE_PROPERTIES` definitions distinguishable
- Add mutation wire definitions for entities, edges, and kind-first mutation envelopes
- Add relation definitions under a root `x-objs-relations` extension in the codegen-only document
- Project schema tags/attributes and relation-rule tags/attributes into a codegen-only
  `x-objs-codegen` metadata block
- Preserve recognized Java overrides: schema `codegen.java.typeName`, relation
  `codegen.java.outboundMethod` / `codegen.java.inboundMethod`, and tags
  `codegen.java.skip` / `codegen.java.noInverse`
- Project schema-level `codegen.baseClass` and comma-separated `codegen.interfaces` values for
  generated Java entity/payload classes
- Project relation-level `codegen.baseClass` for wildcard endpoint bindings and preserve the rule
  metadata even when no static binding can be generated
- Add deterministic read-navigation metadata for outbound and inverse accessors
- Include generated definition keys and `(type, schemaVersion)` identity needed to hydrate typed nodes
- Include sufficient historical `(type, schemaVersion)` metadata for snapshot reads; write-oriented
  latest-type selection must not make older snapshots unreadable
- Include source/target types, role, property policy, property-schema reference, empty-property
  policy, and cardinality
- Preserve property-policy precedence in the manifest: `SCHEMA` carries the edge-property schema
  contract, while `NONE` explicitly describes a bare edge with no property input
- Define stable Java symbols and collision rejection
- Define that absent overrides retain the current `jsonSchemaDefKey`, relation-property, and inverse
  relation-property naming fallbacks
- Support 2020-12 and draft-07 definition/ref layouts
- Preserve `$defs` for 2020-12 and `definitions` for draft-07, including dialect-correct `$ref`
  prefixes and `allOf` wrapping where draft-07 disallows meaningful `$ref` siblings
- Keep linked relation properties explicitly read-only
- Keep the export and manifest schema-generic; application-specific generated classes are produced
  only by the consuming application's build
- Add exporter and REST contract tests

## Out of scope

- Generator source code
- `jsonschema2pojo` plugin changes
- API module implementation
- Generated application sources in any root `objs-*` module
- Automatic graph materialization

## Implementation evidence

- `FullCatalogJsonSchemaExporter.exportForCodegen` keeps entity and edge-property payload schemas
  under the dialect-native definitions keyword, while removing synthetic relation properties from
  the write profile.
- The codegen-only document contains `x-objs-relations`, `x-objs-codegen`, historical exact
  `(type, schemaVersion)` metadata, generic mutation wire definitions, and non-blocking edge
  property diagnostics.
- Schema and relation attributes/tags control Java names, inheritance, inverse generation, and
  wildcard static-binding eligibility. Standard `export` output remains unchanged.

## Acceptance

- [x] Existing outbound/linked export behavior remains compatible
- [x] New relation and mutation metadata appears only in the JSON-schema-codegen format
- [x] Write profile contains entity payloads without synthetic relation fields
- [x] Every exact non-wildcard rule has deterministic relation metadata
- [x] Read-side navigation metadata identifies direction, role, target/source definition, and edge-property type
- [x] Wildcard rules are represented for dynamic use or explicitly excluded from typed output
- [x] Wildcard relations with a base class expose deterministic endpoint metadata; those without
  one remain runtime-only
- [x] Missing or mismatched edge-property references are surfaced as diagnostics with generic
  property fallback; symbol collisions still fail deterministically
- [x] `SCHEMA` and `NONE` relation metadata are distinguishable and deterministic
- [x] Valid schema/relation overrides are exported deterministically and invalid/blank overrides
  fail clearly
- [x] Base-class and interface metadata is exported without changing the standard catalog format
- [x] Both dialects expose correct refs and definitions
- [x] Both dialects remain standards-valid and consumable by jsonschema2pojo

