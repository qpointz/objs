# Gaps — policy-drools (C-26)

**WI-001 process:** **complete** 2026-09-04 — G-P18–20 `resolved`. See Decision log.  
Assumes C-24 flat evaluator shipped.

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P18 | Drools fact model | **resolved** | EntityFact / EdgeFact + ObjectFact; see Decision log |
| G-P19 | Drools version & deps | **resolved** | See Decision log — poll BOM; minimize declared deps |
| G-P20 | Thread-safety / isolation | **resolved** | Per-call session; KB cache by single policy revision |

## Philosophy (inherited)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P37 | First engine Drools | **resolved** (intent) | Module `:objs-policy-drools` |
| G-P38 | No product rules in foundation | **resolved** | Fixtures / examples only |

## Decision log

| # | Decision | Date | Summary |
|---|----------|------|---------|
| G-P19 | Drools version & deps | 2026-09-04 | **Pin** `org.drools:drools-bom` from Maven Central at story implement time (polled **10.2.0** 2026-09-04 → `libs.versions.toml` `drools`). **Declared** on `:objs-policy-drools` only: `platform(drools-bom)` + **`org.drools:drools-engine`** (+ **`drools-xml-support`** — proven required for programmatic `KieModuleModel` / `writeKModuleXML`). **Do not** declare `kie-api`, `drools-compiler`, `drools-core`, etc. separately unless a compile gap forces it. **Do not** add `drools-engine-classic`, `drools-mvel`, `drools-ruleunits-engine`, or `kie-ci` unless a concrete adapter need is proven. **Isolation:** Drools must never appear on `:objs-policy-api` / `:objs-policy-core` classpaths; consumers opt in via `:objs-policy-drools`. |
| G-P20 | Thread-safety / isolation | 2026-09-04 | **Session:** new `KieSession` per `PolicyEngine.evaluate` call; fire; dispose — never share sessions across threads/calls. **KB cache:** keyed by **single policy revision** (`Policy.id` or `(name, version)`); share `KieContainer`/`KieBase` across calls. Facts do not affect the KB key. **Not in S1:** multi-policy composite KB keys, suite packaging, fancy eviction. Within one orchestrator pass, same revision reuses the cached KB (no recompile). |
| G-P18 | Drools fact model | 2026-09-04 | **Revised (experiment):** insert dedicated **`EntityFact`** / **`EdgeFact`** from the resolved fragment (metadata: `type`, `schema`, `schemaVersion`, `annotations` + payload/properties). Wired sidecar bag → **`ObjectFact`** (named values map). `schema` equals catalog `type` when projecting from domain. Multi-policy packaging N/A (G-P20). |
