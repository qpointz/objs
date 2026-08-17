# Story: Multi-BOM application versions

**Slug:** `multi-bom-app-versions`  
**Branch:** `multi-bom-app-versions`  
**Status:** in-progress  
**Folder:** [`docs/workitems/in-progress/multi-bom-app-versions/`](.)  
**Backlog:** [D-8](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Design (product):** [`docs/design/sbom/example.md`](../../../design/sbom/example.md)  
**Design (engineer mapping):** [`GRAPH-AND-RETRIEVAL.md`](GRAPH-AND-RETRIEVAL.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Extend the SBOM inventory app so an **application version** is a **combination of BOMs** (incomplete / partial bills). **Combined SBOM** is the complete union (select all; **ephemeral**, not stored on the version). Fingerprint, MI, depends-on, and CycloneDX of “the version” use that full union. Support **multiple parallel drafts**, each with a **target version** and **based-on** lineage — still on `sbom_application_version` (no new draft table). Progressive UI: a version with one BOM behaves as today; multi-BOM chrome appears only when count ≥ 2.

## Glossary (product language)

| Term | Meaning |
|------|---------|
| **Application** | Inventory app; metadata includes name, description, **tags**. |
| **Application version** | DRAFT or RELEASED row. DRAFT `version` = **target** semver. Own **tags**. No persisted Combined graph. |
| **BOM** | One **incomplete** constituent for a version; own graph + name, description, **tags**. Default name `BOM` (hidden at count = 1). |
| **Combined SBOM** | Complete union of **all** BOMs for a version (select all). Ephemeral, read-only. No stored metadata row. Combined **tags** live **below the app name** (view mode), not on this view. |
| **Fingerprint** | Immutable snapshot of the **full Combined SBOM** union. **name** + **category** (`approval` \| `history` \| `unknown`). |

## Normative locks

| Topic | Lock |
|-------|------|
| Cardinality | Application **1→\*** versions; version **1→1..\*** **BOMs**. Cannot delete the last BOM (delete the DRAFT instead). |
| Delete | **DRAFT** may be deleted entire (BOMs + that draft’s fingerprints). If other drafts are based on it (or its fingerprints), **confirm a list** then **delete those dependents too** (G-Q12). BOM delete on overview **BOMs list**. RELEASED not deleted this story. |
| Constituent | Own named graph; editable only when parent version is **DRAFT**. Product name **BOM**. Metadata: **name** (unique within that version), **description**, **tags**. No `source` field. |
| Tags | Application, version, and each BOM have their own `tags` list (trim, drop blanks, de-dupe case-sensitive). **App+Ver+BOMs** = unique union of the **open version** (computed). Shown **below the app name in view mode** only. Persist as native string arrays: Postgres `TEXT[]`, H2 `VARCHAR ARRAY` (G-Q8). Not JSON. |
| Aggregate | **Ephemeral** union of selected BOM graphs (G-A3). Never stored on the version. Combined SBOM = select all. Always **read-only**. Fingerprint persists a snapshot of the full union. |
| Multi-draft | Many `DRAFT` rows per app. Target semver in `version` while DRAFT (**may rename**, uniqueness). `based_on_version_id` / `based_on_fingerprint_id`. Unique `(application_id, version)` when version set. |
| New application | **Target version** is **required** on the New application form; bootstrap DRAFT uses it (`fromVersionId` null). One empty BOM named **BOM** (name hidden at count = 1). |
| New draft | Modal: based-on (**RELEASED**, **DRAFT**, or **fingerprint**) + target. If based-on **version** has **>1** BOM, ask **combine into a single BOM**. Fingerprint based-on: always one BOM from that snapshot; no combine question. |
| Promote | Modal: **re-type** version to confirm; may **override** stored target if uniqueness holds. Then DRAFT → RELEASED with that string. |
| Progressive UI | Count = 1 → today’s chrome (app/version tags still shown). Count ≥ 2 → Combined SBOM, left-pane **BOM** multi-select, `{app} / {bom}` root. **Create BOM** on overview is the 1→2 entry (DRAFT only). Shrink 2→1: chrome **off immediately**; open the remaining BOM. Unsaved edits: **block** switch; **Stay** or **Leave**. |
| Combined label | **Combined SBOM** = complete (select all). **BOM** = incomplete part. Virtual; string not reserved as a BOM name. |
| Fingerprint | **Always** Combined SBOM snapshot. **Button** (not a link) opens create modal: **name** + **category** (`approval` \| `history` \| `unknown`). Migrate `note` → name, category `unknown`. |
| CycloneDX UI | **Hide** the application-detail CycloneDX download link this story. Export API may stay. |
| Latest (portal / API / MI) | Highest **RELEASED** by **SemVer 2.0** (`version_serial`). Portal, `GET …/latest`, **MI**, **depends-on**, CDX-of-latest. Drafts never included. No RELEASED → no latest. |
| Demo | ~50% apps single BOM; ~50% 2–3 BOMs. Portal: content = latest RELEASED + multi-BOM cue; footer = total BOMs (all versions) · total versions, lazy per app. |
| Out of story | CDX/SPDX **import**; transactional Save (**D-6**); file demo seeds (**D-7**); foundation `copyGraph` API; UI kit. |

## User journeys (delta vs D-2)

1. **New application:** name, description, **required target version**, tags. Lands on that DRAFT (one hidden **BOM**).  
2. Open application overview (left-pane root). Edit app tags. Open a version; edit version tags when DRAFT.  
2. **Create BOM** (DRAFT overview): name, description, tags. After create, that BOM is Open.  
3. With ≥2 BOMs: Combined SBOM (select all) + left-pane multi-select; Combined SBOM is read-only; edit only a single selected BOM.  
4. **New draft:** based-on (released, draft, or fingerprint) + target; combine-into-single-BOM only when based-on version has >1 BOM.  
5. **Fingerprint:** header **Button** → name + category modal; always Combined SBOM snapshot. No CycloneDX link in the header.  
6. Applications portal: content shows latest RELEASED + multi-BOM if that version has ≥ 2 BOMs; footer totals (all BOMs · all versions) lazy-loaded per app.

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | Story folder, GAPS, trackers |
| 1 — Product + graph design | WI-001, WI-002 | done | Glossary locked; GRAPH-AND-RETRIEVAL |
| 2 — Persistence + domain | WI-003, WI-004 | done | Flyway; ephemeral union; multi-draft; fingerprint columns |
| 3 — API | WI-005 | done | REST + OpenAPI |
| 4 — UI | WI-006 | done | Progressive disclosure + drafts + fingerprint modal |
| 5 — Demo + portal + living docs | WI-007, WI-008 | done | Seeder, Applications portal, design README |

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Product design + glossary (`WI-001-product-glossary.md`)
- [x] WI-002 — Graph and retrieval mapping (`WI-002-graph-and-retrieval.md`)
- [x] WI-003 — Flyway BOMs, multi-draft, tags, fingerprint (`WI-003-persistence.md`)
- [x] WI-004 — Ephemeral Combined SBOM + domain services (`WI-004-domain-services.md`)
- [x] WI-005 — REST constituents + multi-draft + fingerprint (`WI-005-rest-api.md`)
- [x] WI-006 — Progressive UI + drafts + metadata (`WI-006-ui.md`)
- [x] WI-007 — Demo seeder + Applications portal (`WI-007-demo-portal.md`)
- [x] WI-008 — Living docs (`WI-008-living-docs.md`)

## Out of scope

- CycloneDX / SPDX **import** of partial SBOMs  
- Transactional Save (backlog **D-6**)  
- File-based demo inventory (backlog **D-7**)  
- Foundation `copyGraph` store API (keep app-local helper)  
- Auth / multi-tenant  
- UI kit extraction  
- CycloneDX **UI** export link (API may remain; G-P9)  
- Tag search / filter on Applications portal (G-Q16)
