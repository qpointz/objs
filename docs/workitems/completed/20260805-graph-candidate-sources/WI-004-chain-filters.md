# WI-004 — Chained filters + DSL parity

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Chains  
**Status:** done  
**Depends on:** WI-003

## Goal

Execute chained matchers as source (stage 0 if capable) + ordered filters; treat `anno-expr` as filter-only. Prove DSL request bodies produce the same subgraphs as before.

## Scope

- Flatten `BoMChainedMatcher` in JDBC reader and in-memory selector.
- Later stages use `matches` over retained candidates; `anno-expr` filter-only.
- Chain parity test: YAML `[anno, anno-expr]` via DSL → expected entity set.

## Out of scope

- Multi-stage re-source / re-pushdown
- DSL syntax changes
- API pagination / result-size caps / sparse HTTP projection

## Acceptance

- [x] Chain `[anno, anno-expr]` covered by selector test
- [x] Existing DSL decode tests remain green
- [x] Selection timeout budget still enforced in reader
