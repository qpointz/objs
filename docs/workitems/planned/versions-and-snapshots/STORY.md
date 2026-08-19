# Story: Versions and snapshots

**Slug:** `versions-and-snapshots`  
**Branch:** (not started — planned only; **no implementation** until asked)  
**Status:** planned  
**Folder:** [`docs/workitems/planned/versions-and-snapshots/`](.)  
**Backlog:** [C-18](../../BACKLOG.md)  
**Depends on:** **C-17 complete** (live `copyGraph` + `mergeGraph`); C-12 `clone()` replaced by this story. **Next:** [C-19](../foundation-after-versions/STORY.md)  
**Design:** [`docs/design/graph/apps-vs-foundation.md`](../../../design/graph/apps-vs-foundation.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

**Scope:** **only** entity/edge versioning and snapshot graphs. Not C-17 lookups. Not lock/visibility flags.

## Goal

Stable **identities**. Each persist appends an immutable **version** and moves **HEAD**.  
**Live graphs** resolve members at HEAD.  
**Snapshot graphs** **pin** `(identity, versionId)` and **reconstruct** from those pins.

Fingerprints/evidence do not add pool rows. Live catalog is identities at HEAD.

## Live vs snapshot

| | Live graph | Snapshot graph |
|--|------------|----------------|
| Members | identities | pins to versions |
| Read | HEAD | pinned version |
| Edit | new version + HEAD move | none (pins do not move) |
| `copyGraph` (C-17) | new live graph, same identities | — |
| `mergeGraph` (C-17) | new live graph, union of identities | — |
| Snapshot API | — | new graph, pin current HEADs |

C-12 pool `clone()` (new ids) is **replaced** by snapshot pins.

## Stages

| Stage | WIs | Consumers | Notes |
|-------|-----|-----------|-------|
| 0 — Scaffold | WI-000 | — | This folder |
| 1 — Design lock | WI-001 | **docs** | Model + persistence sketch |
| 2 — Version store | WI-002 | — | Identity, version, HEAD, persist |
| 3 — Snapshots | WI-003 | — | Pin + reconstruct |
| 4 — Workbench | WI-004 | **workbench** | Open live vs snapshot |
| 5 — SBOM | WI-005 | **SBOM** | Fingerprint = snapshot; keep-split draft = `copyGraph`; combine draft = `mergeGraph` |
| 6 — Docs | WI-006 | **docs** | Sweep |

AR: live collections stay HEAD; **no AR snapshot product** in this story.

## Work Items

- [ ] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [ ] WI-001 — Design lock (`WI-001-design-lock.md`)
- [ ] WI-002 — Version store (`WI-002-version-store.md`)
- [ ] WI-003 — Snapshot pin + reconstruct (`WI-003-snapshots.md`)
- [ ] WI-004 — Workbench (`WI-004-workbench.md`)
- [ ] WI-005 — SBOM fingerprint (`WI-005-sbom.md`)
- [ ] WI-006 — Living docs (`WI-006-living-docs.md`)

## Out of scope

- Implementation until explicitly asked
- C-17 catalog helpers, reverse lookup, identity query, `copyGraph` / `mergeGraph`, paging, timestamps
- C-20 text `q` / contains
- `writeHold` / `catalogScope` / annotation isolation
- AR collection snapshot product
- Per-keystroke versions; version GC; product semver of components
- AuthZ

## Acceptance (when implemented)

- Live edit does not change a snapshot that pinned the previous version
- Snapshot does not create extra pool identities
- SBOM fingerprint reconstructs pin-time payloads
- `copyGraph` / `mergeGraph` still share identities at HEAD
