# WI-010 — Seeds, demo data, docs

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete  

## Goal

Demo dataset and durable docs reflecting the inventory product: Applications vs Portfolios chrome, portfolio-scoped MI, weak CDX.

## Deliverables

- [x] Demo inventory seeder (`demo` profile): multiple apps with versions, shared assets, duplicate identity, portfolio tree with nested subject areas  
- [x] Dataset ready for MI-1…MI-4 over a selected level (shared Jackson/Boot; duplicates); full report execution is WI-013  
- [x] `docs/design/sbom/example.md` run pointer + [`examples/sbom/README.md`](../../../examples/sbom/README.md)  
- [x] README for tabs and how to run a report  

## Acceptance

- [x] Fresh `./gradlew :sbom-service:run` shows searchable apps/assets and a portfolio ready for MI level selection  
- [x] Docs match shipped behaviour; no foundation jargon in user-facing README  
