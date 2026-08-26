# WI-003 — export-verify + dry-run

**Story:** [`STORY.md`](STORY.md)  
**Gaps:** G-3, G-11  

## Goal

`make export-verify` and a documented dry-run proving the export builds.

## Tasks

- [x] `export-verify`: npm `ci`+`build` in each UI module, then `./gradlew clean build` inside `OUT_DIR`
- [x] Grep checks: `org.poc.objs` in `META-INF/**`, `objs.poc.org/v1` anywhere in export tree
- [x] **Seed export round-trip:** `./gradlew :{prefix}-core:test --tests '*SeedImporter*'` in export tree
- [x] Dry-run with sample `TARGET_PACKAGE=com.example.demo` recorded in WI notes or README
- [ ] Boot smoke optional: `./gradlew :{prefix}-app:run` manual checklist

## Notes

Dry-run (2026-08-26):

```bash
make export TARGET_PACKAGE=com.example.demo OUT_DIR=/tmp/bom-export-test
make export-verify OUT_DIR=/tmp/bom-export-test MODULE_PREFIX=demo
```

Result: grep checks pass; three UI npm builds pass; `./gradlew clean build` pass; SeedImporter tests pass.

## Acceptance

- [x] Documented one-liner dry-run passes verify on maintainer machine
- [x] G-3 closed (UI build in verify path)
- [x] G-11 verified (four `.imports` FQCNs match renamed autoconfig classes)
