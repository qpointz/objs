# Gaps — source export

Track export-plan gaps here. Promote to WIs when scheduling work; defer with explicit lock in [`STORY.md`](STORY.md).

## Open (must resolve or explicitly defer before story close)

| ID | Topic | Status | Notes |
|----|-------|--------|-------|
| G-1 | Non-package `objs` identifiers | **open** | v1 unchanged: REST `/api/v1/objs/**`, domain APIs (`/api/v1/inventory/**`, asset-repository paths), SPA mounts (`/workbench`, `/sbom`, `/ar`), config prefixes (`objs.seeds`, `objs.flyway`, `objs.catalogs`), JDBC/env `OBJS_*` — add Makefile overrides if destination requires |
| G-2 | `Objs*` class simple names | **open** | Package rename only; FQCN changes, short names stay |
| G-3 | UI modules + build | **open** | Three SPAs: `:objs-service-ui`, `:sbom-service-ui`, `:asset-repository-service-ui` (not `objs-service/ui`); rsync excludes `dist/` + `node_modules/` — `export-verify` must npm-build **each** UI module |
| G-5 | Naive string replace | **open** | Longest-first in `generate-config.py`; never bare `objs` replace; watch `objs.poc.org/v0` negative-test in `SeedImporterTest` (must stay rejected version, not partially rewritten) |
| G-6 | qpointz `move_package` fragility | **open** | Prefer copy/rename once; test on fixture tree (WI-001) |
| G-7 | Doc link cleanup | **open** | AGENTS.md → `docs/workitems/RULES.md`; design docs may link completed workitems (WI-004) |
| G-8 | Tool deps (rsync, python+pyyaml, npm) | **open** | Document in `scripts/export/README.md`; optional `check-tools` |
| G-9 | No git in export output | **open** | Destination `git init` manual; out of scope v1 |
| G-11 | Spring Boot SPI `.imports` | **open** | **Four** files: `objs-core`, `objs-service`, `objs-gremlin-service`, **`examples/sbom/sbom-service`** — verify in WI-003 |
| G-13 | Seed `apiVersion` replace + export round-trip | **open** | **Existing** classpath seed YAML rewritten during clean; `SEED_API_VERSION_V1` + `tools/objs_seed.py` + `tools/objs_seed_example.py`; export via handlers/REST must emit same `{API_VERSION}` |
| G-15 | Module inventory vs `settings.gradle.kts` | **open** | Current tree: `:objs-core`, `:objs-service`, `:objs-service-ui`, `:objs-gremlin-*`, `:objs-service-app`, `:sbom-service`, `:sbom-service-ui`, `:asset-repository-service`, `:asset-repository-service-ui` + `projectDir` under `examples/` — `generate-config` must list all `move_dir` + `:project` refs |
| G-16 | `examples/` directory layout | **open** | Package move under `examples/sbom/...` and `examples/asset-repository/...` in addition to top-level modules; Gradle `projectDir` remapping must stay consistent after renames |
| G-17 | `tools/` directory | **open** | Include in rsync; transform `objs_seed.py` + `objs_seed_example.py`; not excluded like `scripts/export/` |

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
