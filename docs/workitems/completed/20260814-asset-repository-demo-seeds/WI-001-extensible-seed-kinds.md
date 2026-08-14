# WI-001 — Extensible seed kinds + Collection handlers

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete

## Goal

Let classpath startup apply custom seed kinds (not only ObjectSchema / AllowedEdgeRule / Graph). Asset-repository collections and objects seed from YAML.

## Deliverables

- [x] `applyOrder` on seed handlers; duplicate `kind` beans fail `SeedImporter` construction
- [x] `CollectionSeedHandler` (`Collection`) and `CollectionObjectsSeedHandler` (`CollectionObjects`)
- [x] Remove `DemoDataSeeder`; demo profile loads ontology + instance YAML
- [x] Seed apply error details in `BoMSeedStartupLoader`
- [x] Docs: `docs/design/graph/seeds.md` extending seed kinds

## Acceptance

- `:asset-repository-service` tests load collections from YAML
