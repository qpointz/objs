# WI-005 — Harden documentation and verification

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Hardening  
**Status:** complete  
**Depends on:** WI-001, WI-002, WI-003, WI-004

## Goal

Make the fragment-to-native-graph architecture discoverable and verify the complete delivery,
including migration of consumers affected by the aligned Gremlin contract.

## Scope

- Update graph design documentation with the fragment policy boundary and reuse map.
- Document the distinction between:
  - `GraphFragment` normalization;
  - persistence `GraphMergePolicy`;
  - validation and mutation semantics;
  - Gremlin result projection;
  - frontend graph rendering.
- Update Gremlin documentation to describe shared resolved input and native materialization parity.
- Document `objs-jgrapht-core` and optional `objs-jgrapht-service` module boundaries.
- Document capabilities, cycle-region DTOs, materialization modes, and service absence behavior.
- Update workbench documentation and tour for conditional algorithm actions and highlights.
- Verify generated codegen consumers can build and use resolved fragments through typed adapters.
- Verify the SBOM `BomUnion` migration and the asset-repository composition migration use the
  shared fragment-policy path, with example-specific identity and persistence policies kept at
  their domain boundaries. Example-facing API changes are allowed; migrate callers and tests
  instead of adding compatibility shims. Do not add SBOM algorithm endpoints or migrate the SBOM
  UI.
- Confirm that JGraphT `GraphImporter` support is excluded from this story and recorded under the
  deferred future-loader follow-up.
- Run focused tests for `objs-api`, typed/codegen consumers, Gremlin, JGraphT core/service, and
  workbench UI.
- Run the repository build and applicable integration checks.
- Confirm no JGraphT dependency appears in browser manifests or bundled assets.
- Confirm no JGraphT or generated application class appears in REST JSON.
- Confirm no Flyway migration is needed.

## Expected touchpoints

- `docs/design/graph/README.md`
- `docs/design/graph/apps-vs-foundation.md`
- `docs/design/graph/gremlin.md`
- `docs/design/ui.md`
- workbench tour files
- codegen consumer test modules
- `examples/sbom/sbom-service/src/main/kotlin/org/poc/objs/sbom/domain/BomUnion.kt`
- `examples/sbom/sbom-service/src/main/kotlin/org/poc/objs/sbom/service/BomGraphSupport.kt`
- `examples/asset-repository/asset-repository-service/src/main/java/org/poc/objs/assetrepository/service/ObjectWriteService.java`
- asset-repository composition integration tests

## Verification

At minimum, run the module-focused tests and UI checks required by the implementation, followed by:

```text
./gradlew test
```

Integration tests should follow repository CI rules and database availability.

## Verification results (2026-09-01)

Focused module tests:

```text
./gradlew :objs-api:test :objs-jgrapht-core:test :objs-jgrapht-service:test \
  :objs-gremlin-core:test :objs-codegen-java:test :sbom-service:test \
  :asset-repository-service:test
```

Repository unit suite:

```text
./gradlew test
```

Codegen consumer examples (composite builds):

```text
cd examples/codegen/jsonschema && ../../../gradlew test
cd examples/codegen/jsonschema-draft07 && ../../../gradlew test
```

Workbench UI:

```text
cd objs-service-ui && npm test
```

Boundary checks:

- **Browser:** no `jgrapht` in `objs-service-ui/package.json` or workbench Vite bundle assets.
- **REST JSON:** algorithm responses use `GraphAlgorithmCapabilities` / `GraphCycleAnalysis` DTOs only
  (no JGraphT or generated application class names on the wire).
- **Flyway:** no schema migration required for this story.
- **Deferred:** JGraphT `GraphImporter` recorded under G-JG-D1 / G-JG-11 in [`GAPS.md`](GAPS.md).

All commands above completed successfully on branch `graph-frontend-jgrapht`.

## Acceptance

- [x] Design and user-facing documentation describe the final fragment and module boundaries.
- [x] The typed/codegen consumer path is verified without moving generated sources into root
      `objs-*` modules.
- [x] Affected Gremlin callers and result consumers are migrated to the aligned contract; backward
      compatibility shims are not required.
- [x] Affected SBOM and asset-repository composition callers are migrated to the resolved-fragment
      contract; breaking example-facing API changes are acceptable.
- [x] Workbench capability absence and conditional actions are documented and tested.
- [x] Browser and REST boundary checks pass.
- [x] Focused and repository-level verification results are recorded.

## Out of scope

- New runtime features beyond fixes required by verification.
- Story closure, archive movement, milestone updates, or backlog closure.
