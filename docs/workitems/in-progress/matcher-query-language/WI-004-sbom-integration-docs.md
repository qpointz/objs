# WI-004 — SBOM integration and design documentation

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Example and documentation  
**Status:** done  
**Depends on:** WI-003; G-A5 in [`GAPS.md`](GAPS.md)

## Goal

Exercise matcher DSL selection through the concrete SBOM example and document the final matcher,
chaining, persistence, safety, and REST contracts.

## Scope

- Integrate the generic matcher selection capability into `objs-sbom-example` without adding a
  parallel SBOM-specific matcher engine.
- Preserve existing SBOM annotation-based routes and graph explorer behavior.
- According to the resolved G-A5 scope, add an advanced matcher example/entry point in the SBOM
  consumer or docs that demonstrates:
  - one `anno` matcher;
  - one `anno-expr` matcher;
  - an ordered chain;
  - JSON and YAML input parity.
- Leave interactive graph-explorer matcher controls to WI-005.
- Add example-module tests proving the canonical SBOM graph can be selected through the generic
  matcher contract.
- Update:
  - [`docs/design/graph/annotations-and-subgraphs.md`](../../../design/graph/annotations-and-subgraphs.md);
  - [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md);
  - [`docs/design/service/rest-api.md`](../../../design/service/rest-api.md);
  - [`docs/design/sbom/example.md`](../../../design/sbom/example.md);
  - relevant public documentation under `docs/public/src/`.
- Document:
  - `anno` / `anno-expr` syntax in JSON and YAML;
  - direct annotation-variable expressions such as
    `version == '1.0.0' && app == 'aapp-lala'`;
  - object versus ordered-array semantics;
  - `BoMChainedMatcher` as an implementation behind `BoMMatcher`;
  - first-child-only pushdown;
  - annotation-only JEXL context and sandbox/limits;
  - migration from the removed matching GET operation to the sole
    `POST /api/v1/objs/graph/query` operation;
  - performance implications of non-pushable expressions.

## Out of scope

- A separate SBOM-only matcher endpoint or DSL
- Changes to the canonical SBOM ontology
- Payload/content expressions
- Graph edge or neighbor traversal
- Graph explorer matcher-mode UI (WI-005)
- Replacing existing UI annotation filters outside WI-005

## Acceptance

- [x] The SBOM example exercises `anno`, `anno-expr`, and a chained matcher end to end
- [x] Existing SBOM routes and graph explorer annotation filters remain compatible
- [x] JSON and YAML examples select the same canonical SBOM entities/edges
- [x] Design docs explain the abstract matcher/composite execution model and pushdown boundary
- [x] Public docs include concise copyable request examples and safety/performance caveats
- [x] Example, OpenAPI, and documentation terminology consistently uses `anno` and `anno-expr`
