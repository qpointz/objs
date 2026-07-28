# WI-001 — Align packages to `org.poc.objs`

**Story:** [`STORY.md`](STORY.md)  
**Status:** pending

## Goal

Rename Maven group and Java packages from `io.qpointz.poc.objs` to **`org.poc.objs`** across
`objs-core` and `objs-service` so new domain code lands in the agreed namespace.

## Scope

- Root Gradle group / any published coordinates
- Java source and test packages under both modules
- Autoconfiguration component scan / imports as needed
- Docs that still claim scaffold-only naming (`AGENTS.md`, design coords) — mark as current

## Out of scope

- Domain model implementation (WI-002+)
- Behavioural REST changes

## Acceptance

- [ ] Build and tests pass with packages under `org.poc.objs.*`
- [ ] No remaining production sources under `io.qpointz.poc.objs`
- [ ] Design/AGENTS package notes match reality
