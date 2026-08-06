# WI-005 — Design docs and SBOM smoke

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Docs + SBOM smoke  
**Status:** done  
**Depends on:** WI-004

## Goal

Normative design docs and concrete-example integration smoke per RULES.

## Scope

- Add `docs/design/graph/gremlin.md` (mapping, **read-only** snapshot, **materialization strategies**, **gremlin-lang**, `traversalOptions`, modules, engine, security, API)
- Update `docs/design/platform/overview.md`, `build-system.md`, `app.md` for `:objs-gremlin-core` / `:objs-gremlin-service`
- [`AGENTS.md`](../../../../AGENTS.md) module list (already lists gremlin modules)
- Link from `docs/design/graph/README.md` and `docs/design/README.md`
- Update `docs/design/service/rest-api.md` (+ service README) for traverse endpoint / owning module
- Finish `docs/design/ui.md` Query section; Explorer **Open in…**
- Document `envelope` vs future `flatten` / `nested-vertices`; reserved `language` / SPARQL-not-on-4.0
- SBOM: document smoke path; no example module code change (G-G17)

## Out of scope

- New backlog features (caps, write-back, Gremlin Server)
- Implementing alternate materialization strategies
- Changing `:objs-sbom-example` ontology/code

## Acceptance

- [x] Design docs describe the shipped behaviour and module split
- [x] Platform / AGENTS module lists include gremlin modules
- [x] REST and UI docs updated
- [x] SBOM/example obligation satisfied via docs + G-G17 skip (no ontology change)
