# WI-003 — Add optional algorithm service endpoints

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Optional HTTP integration  
**Status:** complete  
**Depends on:** WI-002

## Goal

Expose JGraphT-backed algorithms through an optional Spring module while keeping the base graph
service usable when the algorithm module is absent.

## Scope

- Create `:objs-jgrapht-service` with Spring Boot autoconfiguration in the style of
  `:objs-gremlin-service`.
- Depend on `:objs-jgrapht-core` and `:objs-core`; do not add algorithm dependencies to the browser
  or base API module.
- Add:
  - `GET /api/v1/objs/graph/algorithms/capabilities`
  - `POST /api/v1/objs/graph/algorithms/cycles`
- Select live graphs, graph matcher scopes, and pinned graph versions using existing
  `GraphStore` conventions.
- Define request DTOs for graph ID, matcher, optional graph version, materialization mode, and
  analysis limits; keep this selection context in the consuming service rather than the fragment or
  core analysis result.
- Build a `GraphFragment`, invoke the configured `GraphFragmentPolicy`, and reject error
  diagnostics before native materialization.
- Advertise supported algorithms and supported `GENERIC`/optional `TYPED` materialization modes.
- Use `GENERIC` by default for HTTP requests.
- Keep response DTOs free of JGraphT and generated application classes.
- Treat the workbench-required DTO fields as the v1 compatibility target; diagnostics and additional
  consumer-oriented fields are additive best-effort data.
- Wire the module into the workbench runner only when explicitly included; the base service runner
  must not require it.
- Coordinate with the Gremlin contract migration; this optional service does not add compatibility
  shims for superseded Gremlin routes or response contracts.

## Endpoint contract

Capabilities are implementation-neutral and should identify the algorithm and supported
materialization modes. Limits and other metadata are optional best-effort additions:

```json
{
  "algorithms": [
    {
      "id": "directed-cycle-regions",
      "materializationModes": ["GENERIC"]
    }
  ]
}
```

The workbench cycle DTO is `GraphCycleAnalysis`: it returns an algorithm ID, cyclic components with
their component IDs, entity IDs, edge IDs, and aggregate counts:

```json
{
  "algorithm": "directed-cycle-regions",
  "components": [
    {
      "id": "00000000-0000-0000-0000-000000000001",
      "entityIds": ["00000000-0000-0000-0000-000000000001"],
      "edgeIds": ["00000000-0000-0000-0000-000000000101"]
    }
  ],
  "stats": {
    "entityCount": 1,
    "edgeCount": 1,
    "cyclicComponentCount": 1
  }
}
```

The request carries the existing graph-selection context (`graphId`, matcher, and optional
`graphVersion`) plus optional `materialization` and analysis limits. `materialization` accepts
`GENERIC` by default or advertised `TYPED` when an optional factory/provider is installed.
Unsupported modes, invalid scope, policy error diagnostics, and malformed requests return stable
client errors. A successful response may include
`GraphFragmentDiagnostic` entries, but the workbench does not depend on their ordering or presence.

## Expected touchpoints

- new `objs-jgrapht-service/build.gradle.kts`
- new service autoconfiguration and controller packages
- `objs-service-app/build.gradle.kts` / application wiring
- REST/OpenAPI tests
- module dependency and absence tests

## Tests

- capabilities with the service installed;
- capabilities absence when the module is not installed;
- live graph scope;
- matcher-selected multi-graph scope;
- pinned graph-version scope;
- generic default materialization;
- generic default mode;
- optional advertised typed mode;
- no-cycle result;
- self-loop and multi-node cycle results;
- conflict and dangling-endpoint diagnostics;
- malformed requests and unsupported modes;
- Gremlin route integration after the shared-input contract migration.

## Acceptance

- [x] The optional service exposes capabilities and cycle analysis without changing base service
      startup when omitted.
- [x] Requests use existing graph selection and version semantics.
- [x] Policy resolution occurs before JGraphT materialization.
- [x] REST JSON contains only stable algorithm DTOs and domain IDs.
- [x] `GENERIC` works without generated application bindings.
- [x] Controller and service tests cover endpoint presence, absence, and failures.

## Out of scope

- Workbench UI changes.
- SBOM algorithm endpoints.
- Browser-side JGraphT or generated typed classes.
