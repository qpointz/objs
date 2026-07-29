# WI-003 — Multi-document seed format and importer

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Seed implementation  
**Status:** done  
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
- Keep catalog write-through caches visible mid-import and rehydrate them on transaction rollback

### `ObjectSchema` document

`ObjectSchema` uses the authoritative DSL from
[`docs/design/graph/object-schema-dsl.md`](../../../design/graph/object-schema-dsl.md).

The handler:

1. parses root-level `type`, `version`, optional `usages`, and `contentSchema`;
2. runs strict DSL normalization before applying any document;
3. registers `BoMSchema(type, version, contentSchema, usages)`;
4. persists only the normalized DSL definition;
5. uses generated JSON Schema for graph validation.

### `AllowedEdgeRule` document

```yaml
apiVersion: objs.poc.org/v1
kind: AllowedEdgeRule
sourceType: Product
role: OWNED_BY
targetType: Organization
propertiesPolicy: SCHEMA
emptyPropertiesAllowed: true
propertiesSchemaType: CanonicalEdge
propertiesSchemaVersion: 1.0.0
```

### `Graph` document

```yaml
apiVersion: objs.poc.org/v1
kind: Graph
name: demo-payments
entities:
  - key: product
    type: Product
    schemaVersion: 1.0.0
    annotations: {}
    payload: {}
edges:
  - key: product-owned-by-org
    source: product
    target: org
    role: OWNED_BY
```

Entity/edge UUIDs are UUIDv5 over `graphName/entity|edge/key` in the Objs seed namespace.
Kind-specific fields are flat at the document root, matching Mill seed resources.

## Out of scope

- Startup resource discovery and fingerprint ledger
- HTTP endpoints
- `REPLACE`, prune, or synchronization semantics

## Acceptance

- [x] Multiple YAML documents parse independently of declaration order
- [x] Schema/rule definitions are available before graph validation
- [x] `ObjectSchema` uses the documented DSL and rejects structurally invalid definitions
- [x] Generated JSON Schema validates imported graph payloads, including nested arrays/objects and INTEGER fields
- [x] Re-importing unchanged logical content creates no duplicate graph records (UUIDv5 identity)
- [x] Omitted records remain unchanged under `MERGE`
- [x] A bad document rolls back all changes from that resource
- [x] Unsupported versions and kinds follow the resolved gap behavior
- [x] Canonical serializer/importer round-trip tests cover all v1 kinds
