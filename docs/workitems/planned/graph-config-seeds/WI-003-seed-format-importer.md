# WI-003 — Multi-document seed format and importer

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Seed implementation  
**Status:** pending  
**Depends on:** WI-002 manual acceptance; G-S1–G-S7 in [`GAPS.md`](GAPS.md)

## Goal

Define a versioned, extensible multi-document YAML format and import schemas, allowed-edge rules,
and initial graph data through one transactional service.

## Scope

- Define canonical documents with `apiVersion` and `kind`
- Implement v1 handlers for:
  - `ObjectSchema`
  - `AllowedEdgeRule`
  - `Graph`
- Parse all documents before application and apply them in dependency order:
  schemas, allowed-edge rules, then graphs
- Implement deterministic `MERGE` behavior and stable graph identity: seed YAML stable textual
  keys mapped to deterministic **UUIDv5** entity/edge ids (G-S1)
- Keep document handlers kind-extensible so new seed kinds can register later without format redesign
- Return a structured import result with document positions, counts, warnings, and errors
- Reject the complete resource transaction when any document fails
- Add canonical serialization for round-trip tests and future export of all seed kinds

### `ObjectSchema` document

`ObjectSchema` uses the authoritative DSL from
[`docs/design/graph/object-schema-dsl.md`](../../../design/graph/object-schema-dsl.md):

```yaml
apiVersion: objs.poc.org/v1
kind: ObjectSchema
metadata:
  type: Component
  version: 1.0.0
spec:
  contentSchema:
    type: OBJECT
    title: Component
    description: Canonical SBOM component payload
    fields:
      - name: name
        schema:
          type: STRING
          title: Name
          description: Component name
        required: true
      - name: size
        schema:
          type: INTEGER
          title: Size
          description: Component size in bytes
        required: false
```

The handler:

1. parses `metadata.type`, `metadata.version`, and `spec.contentSchema`;
2. runs strict DSL normalization before applying any document;
3. registers `BoMSchema(type, version, contentSchema)`;
4. persists only the normalized DSL definition;
5. uses generated JSON Schema for graph validation.

Canonical export emits the normalized DSL, including derived object `required` lists when present.
Raw JSON Schema is neither an import kind nor an escape hatch in v1.

## Out of scope

- Startup resource discovery and fingerprint ledger
- HTTP endpoints
- `REPLACE`, prune, or synchronization semantics

## Acceptance

- [ ] Multiple YAML documents parse independently of declaration order
- [ ] Schema/rule definitions are available before graph validation
- [ ] `ObjectSchema` uses the documented DSL and rejects structurally invalid definitions
- [ ] Generated JSON Schema validates imported graph payloads, including nested arrays/objects and INTEGER fields
- [ ] Re-importing unchanged logical content creates no duplicate graph records (UUIDv5 identity)
- [ ] Omitted records remain unchanged under `MERGE`
- [ ] A bad document rolls back all changes from that resource
- [ ] Unsupported versions and kinds follow the resolved gap behavior
- [ ] Canonical serializer/importer round-trip tests cover all v1 kinds

