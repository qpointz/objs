# WI-003 — export-verify + dry-run

**Story:** [`STORY.md`](STORY.md)  
**Gaps:** G-3, G-11  

## Goal

`make export-verify` and a documented dry-run proving the export builds.

## Tasks

- [ ] `export-verify`: npm `ci`+`build` in `{prefix}-service/ui`, then `./gradlew clean build` inside `OUT_DIR`
- [ ] Grep checks: `org.poc.objs` in `META-INF/**`, `objs.poc.org` anywhere in export tree
- [ ] **Seed export round-trip:** after `./gradlew :objs-core:test --tests '*SeedImporter*'` (or slice test calling `CanonicalSeedSerializer.serializeCatalogs()`), assert serialized YAML contains `{API_VERSION}` and not `objs.poc.org/v1`; optional HTTP check on registry/graph seed export if app boots
- [ ] Dry-run with sample `TARGET_PACKAGE=com.example.demo` recorded in WI notes or README
- [ ] Boot smoke optional: `./gradlew :{prefix}-app:run` manual checklist

## Acceptance

- Documented one-liner dry-run passes verify on maintainer machine
- G-3 closed (UI build in verify path)
- G-11 verified (four `.imports` FQCNs match renamed autoconfig classes)
