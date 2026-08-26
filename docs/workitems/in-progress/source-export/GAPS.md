# Gaps — source export

Track export-plan gaps here. Promote to WIs when scheduling work; defer with explicit lock in [`STORY.md`](STORY.md).

## Open (must resolve or explicitly defer before story close)

| ID | Topic | Status | Notes |
|----|-------|--------|-------|
| G-1 | Non-package `objs` identifiers | **open** | v1 unchanged: REST `/api/v1/objs/**`, domain APIs (`/api/v1/inventory/**`, asset-repository paths), SPA mounts (`/workbench`, `/sbom`, `/ar`), config prefixes (`objs.seeds`, `objs.flyway`, `objs.catalogs`), JDBC/env `OBJS_*` — add Makefile overrides if destination requires |
| G-2 | `Objs*` class simple names | **open** | Package rename only; FQCN changes, short names stay |
| G-3 | UI modules + build | **resolved** | `export-verify` npm-builds `:objs-service-ui`, `:sbom-service-ui`, `:asset-repository-service-ui` (renamed with prefix) |
| G-5 | Naive string replace | **resolved** | Longest-first in `generate-config.py`; bare example module names omitted to avoid double-prefix; slash-form classpath paths |
| G-6 | qpointz `move_package` fragility | **resolved** | Copy-then-delete deepest-first; fixture self-check in `test_fixture.py` |
| G-7 | Doc link cleanup | **resolved** | generate-config strips AGENTS workitems links; `../../workitems/` removed from design docs |
| G-8 | Tool deps (rsync, python+pyyaml, npm) | **resolved** | Documented in `scripts/export/README.md`; `check-export-tools` + `test-export-fixture` |
| G-9 | No git in export output | **open** | Destination `git init` manual; out of scope v1 |
| G-11 | Spring Boot SPI `.imports` | **resolved** | Verified in WI-003 dry-run (four files, renamed FQCNs) |
| G-13 | Seed `apiVersion` replace + export round-trip | **resolved** | In-place YAML + `SEED_API_VERSION_V1` + SeedImporter tests in export tree |
| G-15 | Module inventory vs `settings.gradle.kts` | **resolved** | All 10 modules in `generate-config.py` |
| G-16 | `examples/` directory layout | **resolved** | Example `move_dir` + Gradle `:project` refs; no bare example module string replace |
| G-17 | `tools/` directory | **resolved** | Included in rsync; `objs_seed.py` transformed |
| G-18 | User cleanup manifest | **resolved** | Template shipped; merge when `cleanup.yml` exists |

## Deferred / out of scope v1

| ID | Topic | Status | Notes |
|----|-------|--------|-------|
| G-10 | Module subset export | **deferred** | Export full repo; optional `MODULES=` flag future |
| G-9 | Git in output | **deferred** | Manual `git init` in destination |

## Resolved

| ID | Topic | Notes |
|----|-------|-------|
| G-4 | jsonschema2pojo generated sources | Not copied; first `./gradlew build` in OUT_DIR regenerates |
| G-12 | UI artifact noise | rsync excludes `node_modules/`, `dist/` for all UI modules |
| G-14 | `UuidV5.OBJS_SEED_NAMESPACE` | Fixed UUID; do not change (deterministic seed ids) |
