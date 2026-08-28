# bom-poc source export

Produces a **renamed copy outside this repository** for destination packaging. The bom-poc tree is never modified.

## Quick start

```bash
# Required: destination Java package
make export TARGET_PACKAGE=com.acme.platform

# Optional output path (default: ../bom-export-<slug>)
make export TARGET_PACKAGE=com.acme.platform OUT_DIR=/tmp/acme-platform

# Verify the export builds (npm UI + Gradle + grep checks)
make export-verify OUT_DIR=/tmp/acme-platform MODULE_PREFIX=platform

# Remove export directory
make export-clean OUT_DIR=/tmp/acme-platform
```

Dry-run sample used during development:

```bash
make export TARGET_PACKAGE=com.example.demo OUT_DIR=/tmp/bom-export-test
make export-verify OUT_DIR=/tmp/bom-export-test MODULE_PREFIX=demo
```

## Variables

| Variable | Required | Default | Purpose |
|----------|----------|---------|---------|
| `TARGET_PACKAGE` | yes | — | e.g. `com.acme.platform` replaces `org.poc.objs` |
| `OUT_DIR` | no | `../bom-export-<slug>` | Export destination (**must be outside repo**) |
| `MODULE_PREFIX` | no | last segment of `TARGET_PACKAGE` | Module prefix (`platform-core`, `:platform-core`) |
| `MODULE_HIERARCHY` | no | empty | Optional Gradle project path prefix, e.g. `:platform:objs`; physical module directories remain flat |
| `API_VERSION` | no | reverse-domain `/v1` | Seed `apiVersion` (`platform.acme.com/v1`) |
| `ROOT_PROJECT_NAME` | no | `MODULE_PREFIX` | `settings.gradle.kts` `rootProject.name` |
| `CLEANUP_CONFIG` | no | `scripts/export/cleanup.yml` | Optional post-transform delete manifest |

### Derived naming example

For `TARGET_PACKAGE=com.acme.platform`:

| Source | Target |
|--------|--------|
| `org.poc.objs` | `com.acme.platform` |
| `org/poc/objs/...` (classpath) | `com/acme/platform/...` |
| `objs-core` | `platform-core` |
| `:sbom-service` | `:platform-sbom-service` |
| `group = "org.poc.objs"` | `group = "com.acme.platform"` |
| `objs.poc.org/v1` | `platform.acme.com/v1` |

## What gets copied

All Gradle modules (foundation, gremlin, workbench runner, SBOM + asset-repository examples), `tools/`, deploy, and selected docs:

- **Included:** `docs/design/`, root `README.md`, `AGENTS.md`, module READMEs
- **Excluded:** `build/`, `node_modules/`, `.git`, `docs/workitems/`, `scripts/export/`, root `Makefile`

## Transform pipeline

1. Validate `OUT_DIR` is outside the repo, then **delete and recreate** `OUT_DIR` (each run is a fresh copy)
2. `rsync` clean copy to `OUT_DIR`
2. `generate-config.py` writes `OUT_DIR/.dumper.yml` from env vars (+ optional cleanup manifest)
3. `dumper.py` runs YAML actions on the copy only:
   - `move_package` (copy-then-delete, deepest paths first)
   - `move_dir` (all modules from `settings.gradle.kts`)
   - `replace_in_files` (longest-first; SPI `.imports`, seeds, Flyway classpath paths, Gradle refs)
   - optional `delete_dir` / `delete_file` from cleanup manifest
   - `delete_empty_folder`

When `MODULE_HIERARCHY` is supplied, Gradle project paths are nested while the exported
module directories remain at the export root. For example, `MODULE_HIERARCHY=:platform:objs`
and `MODULE_PREFIX=platform` generates `:platform:objs:platform-core` mapped to
`platform-core`.

## Optional cleanup manifest

Copy [`cleanup.yml.template`](cleanup.yml.template) to [`cleanup.yml`](cleanup.yml) (gitignored) and list paths **relative to `OUT_DIR`** to strip from the destination copy:

```yaml
delete_dirs:
  - examples/asset-repository
  - deploy/local-dev
delete_files:
  - AGENTS.md
```

Populate after your first dry-run (see story G-18).

## Tool dependencies

- `rsync`
- `python3` + PyYAML (`pip install pyyaml`)
- `npm` (for `export-verify` UI builds)
- `rg` (ripgrep, for verify greps)

Run `make check-export-tools` or `make test-export-fixture` to validate the dumper on a tiny fixture tree.

## Verification checklist

`make export-verify` runs:

1. Grep: no `org.poc.objs` in `META-INF/**`
2. Grep: no `objs.poc.org/v1` (v0 negative-test strings are OK)
3. Grep: no `:objs-*` Gradle refs or unprefixed example modules
4. Grep: no `docs/workitems/` links in exported docs
5. `npm ci && npm run build` in each UI module
6. `./gradlew clean build` in `OUT_DIR`
7. `./gradlew :{prefix}-core:test --tests '*SeedImporter*'`

## Intentionally unchanged (v1)

Per [`GAPS.md`](../../docs/workitems/in-progress/source-export/GAPS.md) G-1:

- REST paths (`/api/v1/objs/**`), SPA mounts (`/workbench`, `/sbom`, `/ar`)
- Config prefixes (`objs.seeds`, `objs.flyway`)
- `Objs*` / `BoM*` class simple names
- JDBC H2 mem names (`jdbc:h2:mem:objs-*`)
- `UuidV5.OBJS_SEED_NAMESPACE` UUID

## Files

| File | Role |
|------|------|
| [`dumper.py`](dumper.py) | qpointz-style action runner (`--root` safety guard) |
| [`generate-config.py`](generate-config.py) | Builds `.dumper.yml` from `TARGET_PACKAGE` |
| [`cleanup.yml.template`](cleanup.yml.template) | Template for optional post-export deletes |
| [`check_out_dir.py`](check_out_dir.py) | Refuses `OUT_DIR` inside bom-poc (used by Makefile before wipe/copy) |
| [`test_fixture.py`](test_fixture.py) | Self-check on minimal fixture tree |
