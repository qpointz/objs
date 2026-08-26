# Story: Periodic source export

**Slug:** `source-export`  
**Branch:** `source-export`  
**Status:** in-progress  
**Folder:** [`docs/workitems/in-progress/source-export/`](.)  
**Backlog:** [P-4](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)  
**Reference:** qpointz `dumper.py` + `.dumper.yml` (external repo; same pattern)

## Goal

Add a **Makefile-driven export** that produces a **clean copy** of the objs codebase **outside** this
repository, renamed for a destination Java package, while **`bom-poc` stays untouched**.

Primary usage:

```bash
make export TARGET_PACKAGE=com.acme.platform
# optional: OUT_DIR=/tmp/my-export MODULE_PREFIX=acme
make export-verify OUT_DIR=../bom-export-com-acme-platform
```

The copy includes **all Gradle modules** (foundation, gremlin, workbench runner, SBOM + asset-repository examples), `tools/`, selected docs (`docs/design/`, root README/AGENTS), seeds, and deploy — **not** `docs/workitems/`.

## Normative locks

| Topic | Lock |
|-------|------|
| Source of truth | **`bom-poc` never modified** by export — rsync to `OUT_DIR`, transform copy only |
| `OUT_DIR` | Must be **outside** repo root; Makefile refuses in-repo paths |
| Primary input | **`TARGET_PACKAGE`** (Java package); module prefix / seed `apiVersion` derived unless overridden |
| Transform engine | YAML-driven **`dumper.py`** (qpointz pattern): `move_package`, `move_dir`, `replace_in_files`, … |
| SPI | **`META-INF/spring/*.imports`** updated via `.imports` in replace pass (not by `move_package`) |
| Docs export | **`docs/design/`** + README/AGENTS + module READMEs; **exclude** `docs/workitems/` |
| Seed apiVersion | **`objs.poc.org/v1` → derived from `TARGET_PACKAGE`** on **existing seed YAML** (in-place during clean), **`SEED_API_VERSION_V1`**, and **export output** |
| Class names | **`Objs*` / `BoM*` types unchanged** in v1 (package move only) |
| Export tooling | Lives in **`scripts/export/`**; excluded from rsync copy |

## Stages

| Stage | WIs | Ready |
|-------|-----|-------|
| 1 — Scaffold + dumper | WI-000, WI-001 | after story creation |
| 2 — Makefile + config gen | WI-002 | after WI-001 |
| 3 — Verify + docs | WI-003, WI-004 | after WI-002 |

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [ ] WI-001 — Dumper script (`WI-001-dumper-script.md`)
- [ ] WI-002 — Makefile + generate-config (`WI-002-makefile-export.md`)
- [ ] WI-003 — export-verify + dry-run (`WI-003-export-verify.md`)
- [ ] WI-004 — Export docs + AGENTS cleanup (`WI-004-export-docs.md`)

## Out of scope (this story)

- Renaming REST paths (`/api/v1/objs/**`), UI mount (`/workbench/`), or config prefix (`objs.seeds`) — see G-1
- Renaming `Objs*` / `BoM*` class simple names
- Subsetting modules (inventory-app without `objs-service`) — see G-10
- CI job / scheduled sync to remote destination repo
- `git init` inside export output

## Acceptance (story)

- `make export TARGET_PACKAGE=…` writes a clean tree outside bom-poc with renamed packages and modules
- `make export-verify` passes `./gradlew build` (+ npm UI build) in the export tree
- `rg 'org\.poc\.objs' --glob '**/META-INF/**'` zero hits in export output
- Story gaps in [`GAPS.md`](GAPS.md) resolved or explicitly deferred
