# WI-002 — Perf harness + README

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Harness  
**Status:** done  
**Depends on:** WI-001  
**Examples:** **AR**

## Goal

Add a stdlib Python harness that times domain REST (and optional Gremlin) against a filled `perf` dataset, and document the operator flow in the AR README.

## Deliverables

- [x] `examples/asset-repository/scripts/ar_perf_harness.py`
- [x] README section: activate `perf`, env knobs, harness invocation, H2 vs Postgres note
- [x] Keep `demo/load-data` documented as optional CSV path

## Out of scope

- Latency SLO assertions in CI
- UI changes
