# Consumers — codegen-write-type-catalog (C-43)

**Normative** (G-W7). Proof / smoke in **WI-004**; recipes doc in **WI-005**.

## Required

| Consumer | Role | After this story |
|----------|------|------------------|
| `:examples/codegen/jsonschema` | **Prove** | Full `EntityCatalog` (+ `EdgeCatalog` if SCHEMA props) conversion tests; identity path via catalog → `IdentityProjection` → store (or harness) |
| `:examples/codegen/jsonschema-draft07` | **Prove** | Dialect parity (same shape as jsonschema) |
| `:sbom-service` | **Smoke** | Regenerate bindings; **one** smoke test: codegen payload → catalog → node/entity and/or identity lookup. No wholesale rewrite of hand-written `model.*Type` |
| `:asset-repository-service` | **Smoke** | Same minimal bar as SBOM |

## Docs (WI-005)

| Doc | Role |
|-----|------|
| **`docs/design/graph/typed-conversion-recipes.md`** (name may tweak) | **Primary** convert recipes: payload ↔ map, → Entity / Node / TypedEntity, ← fromEntity, identity + validate/create sketches |
| `codegen-and-builder.md` / `api-and-codegen.md` | Artifact ownership + pointers to recipes |
| `programmatic-recipes.md` / `persist-sketch.md` | Cross-links only (store mutate stays there) |

## Not consumers

| Surface | Why |
|---------|-----|
| Workbench `/api/v1/objs/**` | Foundation REST; not app ontology codegen |
| Full migration of SBOM `org.poc.objs.sbom.model.*Type` | Out of story; smoke uses `codegen.generated` path |
| Foundation Spring `TypedObjectService` | App-owned; recipes show the pattern only |
