# Gaps — codegen-schema-evolution (C-35)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

**WI-001 must close every `open` row** (resolve or defer) before implementation WIs. Drafts in [`DESIGN.md`](DESIGN.md) are **not** final locks until then.

---

## Provisional (likely keep)

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-E0 | Consumer surface | **resolved** (provisional) | Latest-only object model (Lane A). No per-version `*Node` / versioned root collections |
| G-E0a | Migration authoring | **resolved** (provisional) | Typed `From → To` steps; **reject** app-facing Map→Map SPI |
| G-E0b | Codegen vs hand split | **resolved** (provisional) | Migration bodies never in generated trees; Lane A/B may regenerate; hand `…migration` accumulates |
| G-E0c | Persist on read | **resolved** (provisional) | Read/view upgrade only; store pin+payload unchanged |
| G-E0d | Serialize any version | **resolved** (provisional) | Raw/`Entity` + catalog validation; typed write stays latest |
| G-E0e | Jackson polymorphism | **resolved** (provisional) | Not used for entity hydrate; registry + bindings remain |

---

## Open (design)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-E1 | Lane B source / scope | **open** | B1 all catalog versions vs B3 migration-declared/allow-list vs B2 hand-only DTOs. Naming: `Product_1_0_0` vs package-per-version. Export: versioned `$defs` vs second snapshot document |
| G-E2 | Latest class identity | **open** | Single physical class shared by Lane A and newest Lane B snapshot vs distinct types + final convert. Prefer single class to avoid dual-latest drift |
| G-E3 | ReadNode schemaVersion | **open** | Keep `schemaVersion` = **stored** pin; expose `effectiveSchemaVersion` separately? Or replace? Diagnostics shape |
| G-E4 | Missing chain / step failure | **open** | Fail-open → raw + diagnostic (aligned with G-17) vs fail-closed → throw. Per-view policy override? |
| G-E5 | Edge-property migrations | **open** | Same typed SPI for SCHEMA edge properties in v1 vs defer to follow-up |
| G-E6 | Explicit persist rewrite | **open** | In-story later WI vs separate backlog item. Must interact with C-14 identifier rules + target-schema validation |
| G-E7 | Hydrate to non-latest target | **open** | Option B (`UPGRADE_TO(target)`). Still typed steps; still no historical OM. Defer after UPGRADE_TO_LATEST? |
| G-E8 | Chain topology | **open** | Linear adjacent-only vs allow explicit long-jump steps vs multi-path DAG (error if ambiguous) |
| G-E9 | SemVer / “latest” | **open** | Use `SchemaVersion` comparer everywhere (export currently may use string max in places). Chain adjacency ordering |
| G-E10 | Validation after upgrade (read) | **open** | Optional validate upgraded map against target JSON Schema before Lane A hydrate vs trust step author |
| G-E11 | SPI language shape | **open** | Java-friendly generic interface + `Class<F>`/`Class<T>` vs Kotlin-first helpers; binary compatibility for `:objs-api` |
| G-E12 | InMemory registry helper | **open** | Ship in `objs-api` vs examples-only |
| G-E13 | Lane A package vs Lane B package | **open** | Same generated root package with naming discipline vs forced separate packages/source sets |
| G-E14 | Lenient Jackson mid-chain | **open** | `FAIL_ON_UNKNOWN_PROPERTIES` policy for fromMap on historical DTOs — prefer strict for migration hops |

---

## Out of story / deferred

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-X1 | Per-version object-model APIs | **cancelled** | Conflicts with minimal consumer surface |
| G-X2 | App-facing Map→Map SPI | **cancelled** | Error-prone; maps stay wire-only |
| G-X3 | Auto-rewrite on mutate | **deferred** | Surprising vs pin-and-keep; see G-E6 for explicit rewrite |
| G-X4 | Inferred migrations from schema diff | **deferred** | Tooling hint only; not a substitute for hand steps |
| G-X5 | Downgrade latest → older pin | **deferred** | Lossy; not required for read-old-as-new |
| G-X6 | Aggregate materializer / HTTP client | **deferred** | C-23 G-19 / G-20 |
| G-X7 | Same-pin in-place catalog Save migration | **deferred** | Not version-to-version; operator/process concern |

---

## Links to prior gaps

| Prior | Relation |
|-------|----------|
| C-23 G-18 | Exact bindings retained; upgrade extends hydrate when chain reaches bound version |
| C-23 G-17 | Lossless raw fallback remains the fail-open baseline |
| C-23 G-30 | Implementation should close with evolved-snapshot fixture |
| C-14 identity | Applies to persist rewrite when `schemaVersion` changes; not to read-view upgrade |
