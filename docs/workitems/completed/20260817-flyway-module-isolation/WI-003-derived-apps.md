# WI-003 — Examples as derived apps

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Derived apps  
**Status:** complete  
**Depends on:** WI-002

## Goal

Treat in-repo examples as embedders: own Boot Flyway, own `V1`, never list objs locations.

## Deliverables

- [x] SBOM: delete Java `V3`…`V8`; one `V1__sbom_inventory` = current inventory schema. Vendor split (`postgresql` / `h2`) for `TEXT[]` vs `VARCHAR ARRAY` (and fingerprint CHECK)
- [x] Asset repository: `V100__ar_collection.sql` → `db/migration/V1__ar_collection.sql` (same DDL)
- [x] `:objs-service-app`: `spring.flyway.enabled=false` (already done in WI-002)
- [x] Derived `application.yml`: SBOM `classpath:db/migration/{vendor}`; AR `classpath:db/migration` — **no** objs paths
- [x] Tests use the same app-only locations; Boot Flyway `baselineVersion` `0` so app `V1` still applies after objs

## Out of scope

- Stepwise example migration chains
- Living design rewrite beyond what tests need (WI-004)

## Acceptance

- SBOM and AR Boot Flyway histories start at `V1` and never apply `bom_*`
- Workbench runner still gets `bom_*` via objs autoconfig with Boot Flyway off
- App DDL that `REFERENCES bom_graph` still applies (objs ran first)
- `:sbom-service:test` and `:asset-repository-service:test` green
