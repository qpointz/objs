# Gaps — codegen-schema-evolution (C-35)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

**Story scope:** **payload / single-entity migration only** (L2). Compaction and decomposition are deferred (G-X8).

**WI-001:** design locked below. Promote into living docs as part of this WI; no product code until WI-002+.

---

## Scope layers

| Layer | Meaning | C-35 |
|-------|---------|------|
| L1 Compaction | Subgraph → single logical payload | **Out** (G-X8) |
| L2 Payload | Same entity identity; typed / map hops | **In** |
| L3 Decomposition | One payload → N entities + edges | **Out** (G-X8) |

L2 must not create or delete entities/edges. Read/view upgrades leave the store pin + payload unchanged (G-E0c).

---

## Locked (consumer + authoring)

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-E0 | Consumer surface | **resolved** | Latest-only object model (Lane A). No per-version `*Node` / versioned root collections |
| G-E0a | Migration authoring | **resolved** | Step **kinds**: `ClassToClassUpgradeStep`, `MapToClassUpgradeStep` sharing `SchemaUpgradeStep.apply(Map)→Map`. **Reject** app-facing Map→Map kind |
| G-E0b | Codegen vs hand split | **resolved** | Migration bodies never in generated trees; Lane A/B may regenerate; hand `…migration` accumulates |
| G-E0c | Persist on read | **resolved** | Read/view upgrade only; store pin+payload unchanged |
| G-E0d | Serialize any version | **resolved** | Raw/`Entity` + catalog validation; typed write stays latest |
| G-E0e | Jackson polymorphism | **resolved** | Not used for entity hydrate; registry + bindings remain |

---

## Locked (L2 design)

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-E1 | Lane B source / scope | **resolved** | **B1:** all catalog ENTITY versions; naming `Product_1_0_0`; dedicated package; **second snapshot export** |
| G-E2 | Latest class identity | **resolved** | Single physical Lane A class as terminal `To`; no duplicate latest Lane B type |
| G-E3 | ReadNode schemaVersion | **resolved** | `schemaVersion` = **stored** pin; `effectiveSchemaVersion` + diagnostics separately |
| G-E4 | Missing chain / step failure | **resolved** | **Fail-open** → raw + diagnostic after explicit chain and additive fallback both fail |
| G-E8 | Chain topology | **resolved** | Adjacent preferred; explicit long-jump OK; multi-path → error |
| G-E9 | SemVer / “latest” | **resolved** | `SchemaVersion` comparer everywhere |
| G-E10 | Validation after upgrade (read) | **resolved** | No mandatory validate-after-upgrade on read |
| G-E11 | SPI language shape | **resolved** | Java-friendly kinds + shared `SchemaUpgradeStep`; package `org.poc.objs.api.typed.upgrade` |
| G-E12 | InMemory registry helper | **resolved** | `InMemorySchemaUpgradeRegistry` in `objs-api` |
| G-E13 | Lane A vs Lane B packages | **resolved** | Forced separate packages / source sets |
| G-E14 | Jackson mid-chain | **resolved** | ClassToClass / DefaultAdditive: **strict** `fromMap`; hand MapToClass may use lenient |
| G-E15 | Additive default fallback | **resolved** | After empty explicit chain, `DefaultAdditiveMapToLatest` (strict Map→latest class). Incomplete chain → raw (no mid-chain patch). Part of `UPGRADE_TO_LATEST` |
| G-E16 | Evidence / examine wire | **resolved** | Fingerprint/frozen BOM: default **both** (as-saved + latest projection). Live inventory default **saved**. Query `representation=saved\|latest\|both` |
| G-E17 | Regression packs | **resolved** | Frozen `evidence.json` + `expect-latest.json` packs; accumulate; foundation + SBOM layers |

---

## Deferred from this story

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-E5 | Edge-property migrations | **deferred** | Entity payloads first |
| G-E6 | Explicit persist rewrite | **deferred** | Separate backlog; C-14 applies |
| G-E7 | Hydrate to non-latest target | **deferred** | `UPGRADE_TO(target)` later; v1: `EXACT_ONLY`, `UPGRADE_TO_LATEST` |
| G-X1 | Per-version object-model APIs | **cancelled** | |
| G-X2 | App-facing Map→Map SPI | **cancelled** | Maps are chain wire only |
| G-X3 | Auto-rewrite on mutate | **deferred** | |
| G-X4 | Inferred migrations from schema diff | **deferred** | DefaultAdditive is compatibility probe, not inferred remaps |
| G-X5 | Downgrade latest → older pin | **deferred** | |
| G-X6 | Aggregate materializer / HTTP client | **deferred** | C-23 G-19 / G-20 |
| G-X7 | Same-pin in-place catalog Save migration | **deferred** | |
| G-X8 | Compaction / decomposition | **deferred** | L1 / L3; no SPI in C-35 |

---

## Links to prior gaps

| Prior | Relation |
|-------|----------|
| C-23 G-18 | Exact bindings retained; upgrade extends hydrate |
| C-23 G-17 | Fail-open raw baseline (after fallback) |
| C-23 G-30 | **Closed** by SBOM Component@1→2 regression + fingerprint dual view (WI-003) |
| C-14 identity | Persist rewrite only (G-E6) |
