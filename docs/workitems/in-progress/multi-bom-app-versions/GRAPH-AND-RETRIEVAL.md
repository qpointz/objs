# Graph and retrieval — multi-BOM versions (D-8)

**Story:** [`STORY.md`](STORY.md)  
**Audience:** engineers implementing WI-003+  
**Status:** normative  
**Product glossary:** [`docs/design/sbom/example.md`](../../../design/sbom/example.md)  
**Gaps:** [`GAPS.md`](GAPS.md)

End-user UI and domain API **must not** expose graph / entity / edge / matcher vocabulary.

---

## Hybrid persistence

```text
sbom_application                    tags[]
  └── sbom_application_version      tags[]; DRAFT|RELEASED; version=target or released;
                                    version_serial; based_on_version_id? | based_on_fingerprint_id?
        ├── Combined SBOM           ephemeral union of selected BOM graphs (not stored)
        ├── sbom_application_sbom[] product: BOM; name, description, tags[], graph_id, sort_order
        │     └── each graph_id → named graph (editable iff parent is DRAFT)
        └── sbom_application_fingerprint[]  name, category (approval|history|unknown)
              └── graph_id → snapshot of full Combined SBOM union
```

| Layer | Owns |
|-------|------|
| Domain tables | Application, version (lifecycle, tags, based-on, serial), BOM rows, fingerprint metadata, portfolios |
| objs named graphs | One graph per **BOM** row; one graph per **fingerprint** snapshot |

**Forbidden:** `sbom_application_version.graph_id` as Combined SBOM. Combined is computed at read time.

---

## Product → storage

| User concept | Storage |
|--------------|---------|
| Application | Domain row + `tags` |
| Version (DRAFT/RELEASED) | Domain row; no Combined graph |
| BOM | `sbom_application_sbom` + named graph |
| Combined SBOM | Union of **all** BOM graphs of that version (select all) |
| Subset multi-select | Same union over **selected** BOM graphs; not labeled SBOM |
| Fingerprint | Domain row + snapshot graph of the **full** union |
| Tags | Domain columns only (not graph payload, not fingerprint hash) |
| Latest | Max `version_serial` among RELEASED |

---

## Union algorithm (G-A3)

Used at **read time** (Combined SBOM, multi-select, MI, depends-on, CDX-of-latest) and when **materializing** a fingerprint or a flatten-on-draft copy.

1. Collect entity membership from each selected BOM graph (pool asset ids). Same pool asset **once**.  
2. Collect edges. Duplicate edges with the same source, target, and role **collapse** to one.  
3. Result is a `BoMSubgraph` (or equivalent) — **not** written back to the version.

Fingerprint / flatten-copy: run the union, then `copyGraph` (app-local helper) into a **new** named graph with the annotations below.

---

## Copy / snapshot

| Action | Graphs |
|--------|--------|
| New draft, keep split | Deep-copy each BOM graph + metadata (`name`, `description`, `tags`, `sort_order`) |
| New draft, combine | One new BOM (`name=BOM`) whose graph is a copy of the **computed full union** |
| New draft from fingerprint | One BOM whose graph is a copy of the **fingerprint** graph |
| New application | One empty BOM graph, `name=BOM` |
| Fingerprint | Copy of computed full union only — no `sbom_application_sbom` rows |
| Delete DRAFT | Delete BOM graphs + fingerprint graphs of that draft **and** dependent drafts (G-Q12) |

---

## Annotations

Reuse D-2 conventions. BOM graphs:

| Key | Value |
|-----|--------|
| `kind` | `application-bom` |
| `applicationId` | application UUID string |
| `versionId` | version UUID string |
| `bomId` | `sbom_application_sbom.id` |

Fingerprint graphs: `kind=application-fingerprint` (existing), plus `applicationId` / `versionId` / `fingerprintId`.

Do **not** use `kind=application-version` for Combined (that graph no longer exists after migration).

---

## Migration (WI-003)

Existing `sbom_application_version.graph_id`:

1. Insert one `sbom_application_sbom` per version: `name=BOM`, `sort_order=0`, `graph_id` = current version graph.  
2. Drop `sbom_application_version.graph_id` (and its unique/FK).  
3. Re-annotate the graph as `application-bom` with `bomId`.

DRAFT rows with null `version` → default `0.1.0` and compute `version_serial`. Fingerprint `note` → `name`, `category=unknown`.

---

## Retrieval entry points

| Capability | Graphs |
|------------|--------|
| Edit BOM (DRAFT, one selected) | That BOM `graph_id` |
| Combined SBOM / CDX of a version | Union of **all** BOM graphs of that version |
| Fingerprint view / CDX | Fingerprint snapshot graph |
| MI / depends-on / CDX-of-latest | Union of BOMs of **latest RELEASED** (`version_serial`); skip apps with no RELEASED |
| Portal stats | Domain counts (lazy per app); `latestMultiBom` = latest RELEASED has ≥ 2 BOM rows |
| Inventory REST | `/api/v1/inventory/applications/**` — BOMs under `.../sboms`, Combined under `.../combined` (GET only), fingerprints require `name`+`category` |

---

## Anti-patterns

- Persisting Combined on the version and rebuilding on every BOM save  
- Copying BOM rows onto a fingerprint  
- Using `promotedAt` for latest  
- Exposing graph ids as primary UX
