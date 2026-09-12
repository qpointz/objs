# Story: Export package-sensitive assets + codegen path guard

**Slug:** `export-package-sensitive-assets`  
**Branch:** `export-package-sensitive-assets`  
**Status:** in-progress  
**Folder:** [`docs/workitems/in-progress/export-package-sensitive-assets/`](.)  
**Backlog:** [P-5](../../BACKLOG.md)  
**GitLab:** [#4](https://gitlab.qpointz.io/sandbox/bom-poc/-/work_items/4)  
**Base:** `origin/dev`  
**Prior:** [P-4 source-export](../../completed/20260828-source-export/STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Fix two export / consumer-codegen failures discovered when using Makefile export under nested
or prefix-renamed layouts:

1. **Package-sensitive non-source files** — export `replace_in_files` missed Drools `.drl` seed
   bodies (`package` / `import` still `org.poc.objs…`), so SBOM policy evaluation fails after
   rename. Confirm no other non-compilable package-sensitive extensions are missing.
2. **Codegen output path guard** — `JavaCodeGenerator.validateOutputDirectory` rejects any path
   segment that `startsWith("objs-")`. That false-positives at any nesting depth, including:
   - `/root/completely-different-folder-name/…`
   - `/root/a/b/…/z/objs-platform/…`
   - prefix-renamed apps such as `objs-sbom-service`

   Intent (C-23): never write generated sources **into a foundation module directory**. Parent
   folders and application modules must remain allowed.

## Normative locks

| Topic | Lock |
|-------|------|
| DRL rewrite | `.drl` is in export `REPLACE_EXTENSIONS`; package/import strings follow `TARGET_PACKAGE` |
| Other non-code | Repo scan: only `.drl` was missing; `.imports` / YAML seeds / `.tsx` / `.md` already covered |
| Codegen guard | Reject only when a path segment **exactly equals** a known foundation module dir name |
| Nesting | Guard must work at any depth under that foundation dir; incidental `objs-*` ancestors OK |
| Export rename | Foundation name literals in the guard set must track `MODULE_PREFIX` via existing string replace |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | This folder + backlog + branch |
| 1 — Export DRL | WI-001 | done | `REPLACE_EXTENSIONS` + fixture |
| 2 — Codegen guard | WI-002 | done | Exact foundation dir set + tests + README |

## Work Items

- [x] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [x] WI-001 — Export rewrite for `.drl` (and confirm no other gaps) — examples: **SBOM policy seeds** (`WI-001-export-drl.md`)
- [x] WI-002 — Codegen foundation-dir path guard — examples: **SBOM / AR / codegen examples** (`WI-002-codegen-path-guard.md`)

## Out of scope

- Expanding export `TOP_LEVEL_MODULES` to move/rename `objs-codegen-java` / policy / jgrapht
- Changing `MODULE_HIERARCHY` physical layout (Gradle paths only; dirs stay flat)
- Class renames (`Objs*` / `BoM*`)

## Acceptance

- [x] Exported `.drl` seeds contain `TARGET_PACKAGE` (no leftover `org.poc.objs`)
- [x] `make test-export-fixture` covers DRL rewrite
- [x] Codegen allows output under nested monorepos and `objs-sbom-service`-style app dirs
- [x] Codegen still rejects output under foundation dirs (e.g. `…/objs-api/…`) at any depth
- [x] `./gradlew :objs-codegen-java:test`

## Process notes

1. One WI at a time; `[x]` + one commit + push per WI.  
2. Do not close this story until the user asks.
