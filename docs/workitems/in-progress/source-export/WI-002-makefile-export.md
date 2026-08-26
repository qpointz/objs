# WI-002 — Makefile + generate-config

**Story:** [`STORY.md`](STORY.md)  
**Gaps:** G-1, G-5, G-8  

## Goal

Root [`Makefile`](../../../Makefile) targets `export`, `export-clean`, and [`scripts/export/generate-config.py`](../../../scripts/export/generate-config.py) that emits `.dumper.yml` into `OUT_DIR` from `TARGET_PACKAGE`.

## Tasks

- [x] `make export TARGET_PACKAGE=…` — validate package, refuse in-repo `OUT_DIR`, rsync with excludes, generate config, run dumper
- [x] Derive defaults: `MODULE_PREFIX` (last segment), `API_VERSION` (reverse domain `/v1`), `ROOT_PROJECT_NAME`
- [x] `move_dir` all modules (see current `settings.gradle.kts`); `replace_in_files` longest-first (package, gremlin subpackage, `:objs-*` / `:sbom-*` / `:asset-repository-*` gradle refs)
- [x] **`replace_in_files` rewrites existing seed YAML** under `examples/**/resources/seeds/` (and any other `**/seeds/**`) — `objs.poc.org/v1` / quoted form → `{API_VERSION}`; no manual regen required for shipped seeds
- [x] **`objs.poc.org/v1`** → `{API_VERSION}` in Kotlin (`SeedModels.kt`, handlers), Python (`tools/objs_seed.py`), test fixtures, design docs
- [x] `API_VERSION` derivation: reverse-domain of package + `/v1` (same value for import validation and export serialization)
- [x] Verify static: `rg 'objs\.poc\.org/v1'` in export tree → zero hits
- [x] Verify export round-trip: seed serializer or `GET .../registry/export?format=seeds` output uses `{API_VERSION}` only (WI-003)
- [x] `delete_dir` / `delete_file` from **[`scripts/export/cleanup.yml`](../../../scripts/export/cleanup.yml)** (template committed; paths relative to `OUT_DIR`; missing file = no extra deletes)
- [x] `make export-clean OUT_DIR=…`
- [x] Optional `MODULE_PREFIX`, `API_VERSION`, `OUT_DIR` overrides

## Notes

- Also replaces slash-form classpath paths (`org/poc/objs/...` → target package path) for Flyway SQL locations.

## Acceptance

- [x] `make export TARGET_PACKAGE=com.example.demo OUT_DIR=/tmp/bom-export-test` completes; bom-poc source tree untouched by transform
- [x] Generated tree has renamed package dirs and module dirs; SPI `.imports` files updated
- [x] `cleanup.yml.template` present; merge into dumper config when `cleanup.yml` exists (empty lists OK)
