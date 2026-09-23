# WI-002 — Runtime SPI + typed hydrate path

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Runtime  
**Status:** done  
**Depends on:** WI-001  
**Examples:** **—** (unit tests in `:objs-api`)

## Goal

Ship foundation contracts in `:objs-api` (`org.poc.objs.api.typed.upgrade`): step kinds, registry, `UPGRADE_TO_LATEST` with additive fallback, hydrate integration, diagnostics.

## Deliverables

- [x] `SchemaUpgradeStep` + `ClassToClassUpgradeStep` + `MapToClassUpgradeStep`
- [x] `InMemorySchemaUpgradeRegistry` (G-E8/G-E9/G-E12)
- [x] `HydrationPolicy`: `EXACT_ONLY`, `UPGRADE_TO_LATEST` (+ additive fallback)
- [x] Wire into `TypedGraphView` (stored pin unchanged; `effectiveSchemaVersion` / diagnostics)
- [x] Foundation unit tests (`SchemaUpgradeTest`)
- [x] No generated migration bodies

## Out of scope

- Lane B export/generator (WI-003)
- SBOM dual wire / seeds (WI-003)
- Persist rewrite (G-E6), edge props (G-E5), G-X8
