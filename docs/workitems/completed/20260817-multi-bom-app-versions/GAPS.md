# Gaps — multi-bom-app-versions (D-8)

Summary tables first. **Open — questions to lock:** none remaining. Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

---

## Architecture (plan locks)

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-A1 | Version vs constituents | **resolved** | Version **1→1..\*** constituents; each constituent has its own named graph |
| G-A2 | Aggregate | **resolved** | **Ephemeral** — never persisted on the version. Union of **selected BOM** graphs at read time (G-A3). **Combined SBOM** = **select all**. Subset multi-select uses the same mechanic (incomplete; not labeled SBOM). Fingerprint may **materialize a snapshot** of the full union into its own graph. |
| G-A3 | Union rules | **resolved** | Union entity membership + edges; same pool asset once; duplicate edges (same source, target, role) collapse |
| G-A4 | Multi-draft storage | **resolved** | Same `sbom_application_version` table; no new draft table; `based_on_version_id` FK on that table |
| G-A5 | Target version | **resolved** | While `status=DRAFT`, `version` column holds **target** semver |
| G-A6 | Uniqueness | **resolved** | Unique `(application_id, version)` when `version` is non-null (covers drafts + released) |
| G-A7 | Fingerprint / MI / CDX | **resolved** | Fingerprint **computes** the full union then **persists a snapshot** graph (never constituent rows). MI / CDX / depends-on of “the version” **compute** the full union at read time (latest RELEASED). No `version.graph_id` Combined graph. |
| G-A8 | Constituent metadata | **resolved** | `name` (required, **unique within the same application version**), `description`, `tags`; no separate `source` field. Same name may exist on other versions / apps. |
| G-A9 | Progressive UI | **resolved** | Extra chrome only when open version has **≥ 2** BOMs; **Create BOM** on overview is the 1→2 path. App/version tags still show at count = 1; BOM name **hidden** at count = 1. Shrink 2→1: chrome **off immediately**; selection forced to the remaining BOM (G-Q10). Count ≥ 2: left pane **multi-select** (G-P3). |
| G-A10 | Combined label | **resolved** | **Combined SBOM** = complete union (**select all**). **BOM** = one incomplete constituent. Subset multi-select is not an SBOM. Combined SBOM string is **not reserved** as a BOM name (G-Q9). |
| G-A11 | Latest | **resolved** | Highest **RELEASED** by **SemVer 2.0** (`version_serial`). Portal, `GET …/latest`, **MI**, **depends-on**, and CDX-of-latest all use this. Drafts never included. No RELEASED → no latest (G-Q11). |
| G-A12 | Tags on app / version / constituent | **resolved** | Each has its **own** `tags` list (trim, drop blanks, de-dupe case-sensitive). Not in the graph; not in fingerprint hash. |
| G-A13 | Combined tags | **resolved** | Unique union App ∪ version ∪ **all BOMs** of the **open version** (first-seen: app, version, BOMs by sort). Computed; not stored. UI: **view mode below the app name** only (G-Q16). Not portal, not search, no second strip on Combined SBOM. |
| G-A14 | Fingerprint metadata | **resolved** | `name` (required) + `category` (`approval` \| `history` \| `unknown`). Replaces `note`. Migrate: name ← note, category ← `unknown`. |
| G-A15 | Tags column type | **resolved** | Native string **arrays**, not JSON. Postgres `TEXT[]`; H2 `VARCHAR ARRAY` (G-Q8). Same type on app, version, constituent. |
| G-A16 | BOM vs SBOM | **resolved** | Product: **BOM** = one incomplete constituent. **Combined SBOM** = complete (select all). App chrome “SBOM inventory” may stay. Persistence/API ids (`sbom_*`, `/sboms`) unchanged this story. |

## Product (plan locks)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P1 | Create BOM | **resolved** | Overview only (left-pane root); DRAFT; modal name/description/tags; after create, new BOM is Open |
| G-P2 | Default open (count ≥ 2) | **resolved** | **Select all** (Combined SBOM), read-only; after Create BOM → select **only** the new BOM |
| G-P3 | Left-pane switch | **resolved** | Compact menu like workbench Schema ▾; **multi-select** when count ≥ 2. One selected → that **BOM** (editable if DRAFT). Two or more but not all → ephemeral union, **read-only**, not labeled SBOM. Combined SBOM = select all. Root `{app}` or `{app} / {bom}` (one) / Combined SBOM (all) |
| G-P4 | Portal card | **resolved** | **Content:** latest RELEASED (G-A11) + **multi-BOM** cue if that version has ≥ 2 BOMs. Omit latest when none. **Footer:** independent totals — **all versions** count, **all BOMs** count (sum across every version). Lazy-load stats **per app** (G-Q14). |
| G-P5 | Demo split | **resolved** | ~50% single / ~50% multi-constituent; deterministic |
| G-P6 | Drafts + multi-BOM | **resolved** | Drafts use same constituent model as any version |
| G-P7 | Draft combine prompt | **resolved** | Creating a draft from a version with **>1** BOM asks whether to combine into a **single BOM**. Yes = one BOM whose graph is a **persisted copy of the computed full union**. Tags = unique union of source BOM tags. No = deep-copy each BOM. Single-BOM source: no question. Flattened name default **BOM** (G-Q13). |
| G-P8 | Fingerprint create | **resolved** | Header **Button** (same family as New draft/Save, not a link / not subtle Anchor). Opens modal: required **name** + **category** (`approval` \| `history` \| `unknown`). Always Combined snapshot. List/menu show name + category pill. |
| G-P9 | CycloneDX link | **resolved** | **Hide** the CycloneDX download link on application detail. Weak export API may remain; no UI this story. |
| G-P10 | New application target | **resolved** | **Target version** is a **required** field on New application (`G-Q1`). |
| G-P11 | Unsaved switch | **resolved** | Block version / fingerprint / BOM selection change when dirty. Confirm **Stay** or **Leave** (G-Q15). |
| G-P12 | Combined tags UI | **resolved** | Read-only **below app name** in view mode (G-Q16). Nowhere else this story. |
| G-P13 | Transactional save | **deferred** | Remains backlog **D-6** |
| G-P14 | File demo seeds | **deferred** | Remains backlog **D-7** |

## Process / foundation

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-X1 | Scope boundary | **resolved** | Example-app only (`:sbom-service` / UI); no foundation copyGraph API this story |
| G-X2 | Import partial BOMs | **cancelled** | CDX/SPDX import out of story |

---

## Resolved in planning (no longer open)

### G-Q1 — First draft target on new application

**Status:** **resolved** — required field on **New application** form. Create API takes `targetVersion`; bootstrap DRAFT uses it (`fromVersionId` null). No implicit `0.1.0`. Existing null `version` rows: migration default in WI-003.

### G-Q2 — Based-on source kinds

**Status:** **resolved** — **RELEASED**, **DRAFT**, or **fingerprint**. Exactly one source on subsequent drafts. New application: no based-on (G-Q1).

- Version source: keep-split vs combine when that version has **>1** constituent (G-P7).  
- Fingerprint source: always copy the fingerprint **aggregate** into **one** constituent. **No** combine question.  
- Persist: `based_on_version_id` **or** `based_on_fingerprint_id` (existing fingerprint table). UI: “based on 1.0.0” / “based on {fingerprint name}”.

### G-Q3 — Promote: override target?

**Status:** **resolved** — Promote modal: user **must re-type** the version identifier to confirm. The typed value **may override** the draft’s stored target if uniqueness still holds (`application_id` + `version`). Hint/label shows the current target. Empty or blank rejected. CONFLICT if another row already has that version.

### G-Q4 — Rename target while DRAFT

**Status:** **resolved** — DRAFT **may rename** `version` (target) via PATCH, subject to uniqueness. Blank rejected. CONFLICT if another row already has that string. RELEASED `version` is immutable (except promote already set it).

### G-Q6 — Delete draft / delete constituent UX

**Status:** **resolved**

- **Delete DRAFT:** yes — entire version row, **all constituents** (and their graphs), and fingerprints of that draft (FK cascade). **RELEASED** cannot be deleted this story.
- **Delete constituent:** on **application overview**, in the **BOMs list** (same place as **Create BOM**). Per-row delete; not on Combined SBOM. **Cannot delete the last** BOM (delete the DRAFT instead). List only exists at count ≥ 2, so the last-BOM rule matches progressive disclosure.
- Confirm dialogs for both deletes (destructive). Delete DRAFT that is a based-on source: **G-Q12** (cascade dependents after listing them).

### G-Q5 — Draft create: copy depth

**Status:** **resolved** — see G-P7. Keep split vs combine-into-single-BOM. Fingerprints always Combined (G-A7).

### G-Q7 — Semver compare algorithm

**Status:** **resolved** — **Strict SemVer 2.0** (incl. pre-release: `1.0.0-rc.1` < `1.0.0`). Kotlin **`fun interface VersionComparer`**: compare two strings **and** map a string → **`version_serial` (`NUMERIC`)** that is a total order matching that compare. Swap the interface later without rewriting callers.

- Product `version` stays the SemVer string (unique per app).  
- Persist **`sbom_application_version.version_serial`**; recompute on create / rename / promote.  
- **Latest** = `ORDER BY version_serial DESC` among RELEASED (Postgres-usable; no Java lib in SQL).  
- Invalid/non-semver: serial sorts last (or below all valid). Backfill serials in WI-003 from existing `version` strings.

### G-Q17 — Fingerprint category list

**Status:** **resolved** — **`approval`**, **`history`**, **`unknown`**. Demo may use `history`; migrated notes use `unknown`.

### G-Q8 — Tags persistence

**Status:** **resolved** — Native string **arrays**, not JSON. Same column type on application, version, and constituent.

H2 **2.4.240** (`MODE=PostgreSQL`) does **not** parse PostgreSQL `TEXT[]`. It does support SQL-standard arrays (`VARCHAR ARRAY`). WI-003 uses the existing Flyway product-name branch (same as `TIMESTAMPTZ` in V3/V4/V7):

- PostgreSQL: `TEXT[]`
- H2: `VARCHAR ARRAY`

JPA: `String[]` / Hibernate `SqlTypes.ARRAY`. Do not hard-code `columnDefinition = "text[]"` (breaks `ddl-auto=validate` on H2). **No JSON fallback.**

### G-Q9 — Constituent name uniqueness

**Status:** **resolved** — Unique **within the same application version** only (`UNIQUE (version_id, name)`). Other versions of the same app (and other apps) may reuse the same name.

- Trim; reject blank.  
- Case-sensitive (same as tags; default unique on VARCHAR).  
- Virtual label **Combined SBOM** is UI-only (complete / select-all) and **not reserved** as a BOM name. Bootstrap / flatten default name is **BOM** (G-Q13).

### G-Q10 — Shrinking back to one constituent

**Status:** **resolved** — **Yes.** When constituent count drops to **1**, multi chrome **disappears immediately** (same as a version that never had 2+). Selection is forced to the remaining constituent; drop `sbom=` from the URL. Combined is gone (virtual row only exists at count ≥ 2). Count = 1 behaves as today again (app/version tags still shown; **Create BOM** still on DRAFT overview).

### G-Q11 — Portfolio / MI “latest”

**Status:** **resolved** — **RELEASED-only, semver-max** — same as G-A11 / portal. `latestReleased` = `ORDER BY version_serial DESC` among `status=RELEASED`. **Not** `promotedAt`. Drafts are never “latest” for MI, depends-on, CDX-of-latest, `GET …/versions/latest`, or the Applications portal pill. App with no RELEASED: no latest graph (skip / null).

### G-Q13 — Empty based-on (brand-new app)

**Status:** **resolved** — Bootstrap DRAFT: `fromVersionId` null → **one empty BOM** (empty named graph). Combined SBOM is **ephemeral** (not persisted). BOM name defaults to **`BOM`**. At count = 1 that name is **not shown**. Flatten-into-single-BOM (G-P7) also defaults the new BOM name to **BOM**.

### G-Q15 — Unsaved edits when switching BOM / version

**Status:** **resolved** — **Block** the switch. Confirm **Stay** or **Leave**. Same for version / fingerprint switch and BOM / Combined SBOM / multi-select change when `versionDirty` or `payloadUnapplied`.

- **Stay** — cancel; keep edits and current selection.  
- **Leave** — discard unsaved edits, then switch. No save-and-leave in this dialog.

### G-Q16 — Combined tags elsewhere

**Status:** **resolved** — Show the App+Ver+all-BOMs unique set **below the application name**, **view mode** (read-only badges). **No other places** this story: not Applications portal, not search/filter, no extra Combined SBOM tags strip.

REST `combinedTags` may still exist to feed that row.

### G-Q12 — Based-on when source later deleted

**Status:** **resolved** — Delete the source **and all dependent drafts**, after the user **confirms** a list of what will go.

**What “dependent” means.** A later draft stores lineage as `based_on_version_id` **or** `based_on_fingerprint_id`. The copy already has its own BOM graphs; based-on is only the pointer. This story can remove a source by **deleting a DRAFT** (G-Q6): that row, its BOMs, and **its fingerprints** go together. **RELEASED** is not deleted. Fingerprint DELETE stays rejected; a fingerprint disappears only with its version.

Dependents of that delete:

- Drafts with `based_on_version_id` = the DRAFT being deleted  
- Drafts with `based_on_fingerprint_id` pointing at a fingerprint of that DRAFT (those fingerprints are about to vanish)  
- **Transitive:** drafts based on those drafts (or their fingerprints), recursively  

**UI.** If there are no dependents: today’s single “delete this DRAFT?” confirm. If there are: list them (target version string; if the link is a fingerprint, show that fingerprint **name** too). Copy: deleting this draft will also delete the listed drafts (their BOMs and fingerprints). Cancel = stay. Confirm = delete the whole tree.

**API / FK.** Service computes the dependent set for the dialog, then deletes (source last or CASCADE). `based_on_version_id` / `based_on_fingerprint_id`: **ON DELETE CASCADE** is OK once the user confirmed. Do **not** SET NULL and leave an orphan “based on …”. Do **not** 409-restrict without offering the cascade confirm.

### G-Q14 — Portal footer copy

**Status:** **resolved** — Footer stats are **independent totals**, not “latest only”:

- **Version count** — every version row (DRAFT + RELEASED)  
- **BOM count** — every BOM row **across all versions** (not only the latest RELEASED)

Copy uses **BOMs** / **versions** (G-A16), e.g. `12 BOMs · 4 versions`. No draft-count line.

**Lazy load per app** (same idea as schema portal `usedIn`): list applications stays thin; each card/row fetches its stats when shown (skeleton until loaded).

**Content area** (not the footer): **latest RELEASED** (G-A11). If that version has **≥ 2 BOMs**, add a **multi-BOM** indication. No RELEASED → no latest in content (draft-only apps). Same cues in list view.

Not combined tags (G-Q16). Not search.

---

## Deferred / out of story

- **D-6** Transactional Save  
- **D-7** File-based demo inventory  
- CDX/SPDX import of partials  
- Strong/certified CycloneDX  
- Auth / roles  
- Cross-version BOM compare UI  
- Tag search/filter (G-Q16: not this story)
