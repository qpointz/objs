# Gaps — multi-bom-app-versions (D-8)

Summary tables first. **Open — questions to lock** need product decisions before or during WI-001/WI-002. Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

---

## Architecture (plan locks)

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-A1 | Version vs constituents | **resolved** | Version **1→1..\*** constituents; each constituent has its own named graph |
| G-A2 | Aggregate | **resolved** | `sbom_application_version.graph_id` = materialized Combined SBOM; rebuild on constituent mutate |
| G-A3 | Union rules | **resolved** | Union entity membership + edges; same pool asset once; duplicate edges (same source, target, role) collapse |
| G-A4 | Multi-draft storage | **resolved** | Same `sbom_application_version` table; no new draft table; `based_on_version_id` FK on that table |
| G-A5 | Target version | **resolved** | While `status=DRAFT`, `version` column holds **target** semver |
| G-A6 | Uniqueness | **resolved** | Unique `(application_id, version)` when `version` is non-null (covers drafts + released) |
| G-A7 | Fingerprint / MI / CDX | **resolved** | Operate on **aggregate** graph of the version |
| G-A8 | Constituent metadata | **resolved** | `name` (required, unique per version), `description`, `tags`; no separate `source` field |
| G-A9 | Progressive UI | **resolved** | Extra chrome only when open version has **≥ 2** constituents; Create SBOM on overview is the 1→2 path |
| G-A10 | Combined label | **resolved** | Product name **Combined SBOM** (virtual, read-only, no metadata) |
| G-A11 | Latest | **resolved** | Highest **RELEASED** by **semver** (not `promotedAt`) for portal pill and `GET .../versions/latest` |

## Product (plan locks)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P1 | Create SBOM | **resolved** | Overview only (left-pane root); DRAFT; modal name/description/tags |
| G-P2 | Default open (count ≥ 2) | **resolved** | Combined Open, read-only; after Create SBOM → new constituent Open |
| G-P3 | Left-pane switch | **resolved** | Compact menu like workbench Schema ▾; root `{app}` or `{app} / {sbom}` |
| G-P4 | Portal card | **resolved** | Latest RELEASED pill beside name; footer: constituent count + version count |
| G-P5 | Demo split | **resolved** | ~50% single / ~50% multi-constituent; deterministic |
| G-P6 | Drafts + multi-BOM | **resolved** | Drafts use same constituent model as any version |
| G-P13 | Transactional save | **deferred** | Remains backlog **D-6** |
| G-P14 | File demo seeds | **deferred** | Remains backlog **D-7** |

## Process / foundation

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-X1 | Scope boundary | **resolved** | Example-app only (`:sbom-service` / UI); no foundation copyGraph API this story |
| G-X2 | Import partial BOMs | **cancelled** | CDX/SPDX import out of story |

---

## Open — questions to lock

Answer in chat or in WI-001; agent flips rows to **resolved**.

### G-Q1 — First draft target on new application

**Status:** open  
**Context:** Creating an application today auto-creates an empty DRAFT with `version = null`. Multi-draft requires a target in `version`.

**Options:**

1. Default target **`0.1.0`** (or `1.0.0`) with no modal on create.  
2. **Require** target in the New application form.  
3. Allow `version` null only for the bootstrap draft until first Save/Promote (exception to uniqueness).

**Prefer (plan):** (1) or (2) — pick one in WI-001.

---

### G-Q2 — Based-on source kinds

**Status:** open  
**Context:** Create draft needs `fromVersionId`.

**Options:**

1. **RELEASED only** as based-on.  
2. RELEASED **or** another **DRAFT** (copy that draft’s aggregate + constituents).  
3. RELEASED, DRAFT, or **fingerprint** (unlikely).

**Prefer (plan):** (2) — allow draft-from-draft for parallel work; confirm.

---

### G-Q3 — Promote: override target?

**Status:** open  

**Options:**

1. Promote **always** uses the draft’s stored target; no override field.  
2. Promote may **override** version string if uniqueness still holds.  
3. Promote requires re-typing the target as confirmation.

**Prefer (plan):** (1) default; (2) optional if cheap.

---

### G-Q4 — Rename target while DRAFT

**Status:** open  

Can the user change `version` (target) on an existing DRAFT via PATCH, subject to uniqueness? Or is target immutable until promote/delete?

---

### G-Q5 — Draft create: copy depth

**Status:** open  
**Context:** Based-on copy must produce editable constituents.

**Options:**

1. Deep-copy **each constituent graph** + metadata rows; rebuild aggregate.  
2. Copy only aggregate graph into one constituent named `Primary`, then user splits (weak).  
3. Copy aggregate + create N empty constituents (bad).

**Prefer:** (1). Confirm for WI-002/WI-004.

---

### G-Q6 — Delete draft / delete constituent UX

**Status:** open  

- May the user **delete** a DRAFT row entirely?  
- Delete constituent: confirm last-constituent forbidden; any confirm dialog?  
- Orphan fingerprints if based-on draft deleted? (Fingerprints hang off version id today.)

---

### G-Q7 — Semver compare algorithm

**Status:** open  

Strict SemVer 2.0 (`1.0.0-rc.1` vs `1.0.0`)? Loose numeric dotted triples only (demo uses `x.y.z`)? Pre-release ordering needed this story?

**Prefer:** Loose major.minor.patch numeric compare matching demo strings; document; defer pre-release until needed.

---

### G-Q8 — Tags persistence

**Status:** open  

H2 + Postgres: `text[]` vs JSON array column? Prefer whatever Flyway already uses elsewhere in the example.

---

### G-Q9 — Constituent name uniqueness

**Status:** open  

Case-sensitive? Trim-only? Collision with virtual label “Combined SBOM” forbidden?

---

### G-Q10 — Shrinking back to one constituent

**Status:** open  

When the user deletes constituents down to **1**, does multi chrome **disappear immediately** (progressive disclosure), and is selection forced to that constituent?

**Prefer:** Yes — count = 1 behaves as today again.

---

### G-Q11 — Portfolio / MI “latest”

**Status:** open  

MI and depends-on already use “latest version” graphs. Confirm they switch to **highest RELEASED by semver** (same as portal), not `promotedAt`. Drafts never included in MI scope unless product says otherwise.

**Prefer:** RELEASED-only, semver-max — align with G-A11.

---

### G-Q12 — Based-on when source later deleted

**Status:** open  

`based_on_version_id`: `ON DELETE SET NULL` vs restrict delete of based-on while drafts reference it?

---

### G-Q13 — Empty based-on (brand-new app)

**Status:** open  

First draft: `fromVersionId` null → empty aggregate + one empty constituent. Confirm; any auto-name (`Primary`)?

---

### G-Q14 — Portal footer copy

**Status:** open  

Exact strings: `3 SBOMs · 4 versions` vs `3 constituents · 4 versions`? Include draft count when > 0 (`2 drafts`)?

**Prefer:** `SBOMs` to match UI tab language; show draft count only when ≥ 1 draft.

---

### G-Q15 — Unsaved edits when switching SBOM / version

**Status:** open  

Discard prompt / block navigation when versionDirty or payloadUnapplied? Same as today’s version switch, or stricter for constituent switch?

---

## Deferred / out of story

- **D-6** Transactional Save  
- **D-7** File-based demo inventory  
- CDX/SPDX import of partials  
- Strong/certified CycloneDX  
- Auth / roles  
- Cross-version BOM compare UI
