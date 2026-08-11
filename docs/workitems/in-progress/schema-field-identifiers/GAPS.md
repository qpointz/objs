# Gaps — schema-field-identifiers (C-14)

Decisions for open design questions. Status: `open` | `resolved` | `deferred` | `accepted-risk`.

## Priority locks (were blocking WI-005)

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-1 | Edge property schemas | **resolved** | Same field flags and immutability rules as entities (G-2 / G-15). `identifier` / `searchable` allowed on `EDGE_PROPERTIES` schemas under the same placement rules. |
| G-2 | Type / schemaVersion change on update | **resolved** | Project stored vs incoming with each side's schema. Freeze only stored identity paths that **remain** `identifier` on the incoming schema (G-15). Upgrade may set newly marked paths; downgrade (or catalog change) that drops `identifier` on a path is allowed; changing/clearing a still-marked path → `IDENTIFIER_IMMUTABLE`. |
| G-3 | Edit-form “exists” signal | **resolved** | Identifier (and edge-property identifier) inputs are read-only when the item’s id is in a client **`persistedIds`** set: ids loaded from server (Open graph, matcher load, Add objects from pool, etc.) plus ids successfully upserted by Save in this session. Brand-new canvas items (New / New linked) stay editable until first successful Save. Server remains source of truth (`isCreateEntity` / edge equivalent). |

## Compat / equality

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-4 | Unknown OBJECT `required` on read | **resolved** | Must not fail deserialize. Confirm `PayloadMapper` / seed YAML ignore unknown properties; if not, strip or configure ignore in WI-002. |
| G-5 | Identity value equality | **resolved** | For each key in the **stored** projection that is still an identifier path on the **incoming** schema, incoming projection value must equal (standard map equality / JSON scalars). Keys only on the incoming side are allowed (G-15). Keys dropped from the incoming identifier set (downgrade) are not compared. |
| G-6 | Partial payload upsert | **resolved** | Writes remain **full payload replace** (current API). Omitting an identifier key that exists on the **stored** projection → `IDENTIFIER_IMMUTABLE`. |
| G-7 | Empty identity map | **resolved** | Stored schema with no `identifier` fields → empty stored projection → immutability check skipped (allows v1→v2 introduce-identifiers). |

## Accepted risks / clarity

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-8 | Catalog changes `identifier` set after data exists | **resolved** | Covered by G-15: newly marked identifier paths may be set once; previously projected paths stay frozen. |
| G-9 | Snapshot / clone | **resolved** | New UUIDs ⇒ create path; copied identifier values are writable again until first save of those new ids. Soft-link / membership reuse keeps same ids ⇒ update path / frozen. |
| G-10 | Text / Expert instance draft | **resolved** | No special Text UX required; server rejects illegal identifier mutations. Visual form uses G-3 read-only. |
| G-11 | Searchable runtime | **deferred** | No matcher, FTS, index, or search API in C-14. Metadata + editor only. |
| G-12 | Dedupe-by-identity / override-identity API | **deferred** | Out of story (STORY out of scope). |
| G-13 | WI-001 vs pre-filled trackers | **resolved** | BACKLOG/MILESTONE already seeded at story creation. WI-001 = verify trackers + **design doc** updates (not re-add backlog rows). |
| G-14 | Remove SBOM `labels` / `attributes` | **resolved** | Drop from seed schemas, `SbomRegistry`, typed payloads, demo script, and SBOM design docs in WI-002 (same story). Objects stay open (`additionalProperties`); leftover payload keys still validate but are no longer modeled. |
| G-15 | Introduce identifiers on schema migrate | **resolved** | Object at schema v1 (no identifiers) may update to v2 (with identifiers) and set those values; v2→v1 downgrade that drops identifier flags is also allowed. Immutability applies only to paths that are set in the **stored** projection **and** still marked `identifier` on the incoming schema. Missing / null / **blank string** values are omitted from the projection (not yet set) and may be filled on update — same rule on UI lock and `BoMValidator` / `BoMIdentityProjection.isUnset`. |

## Open (none blocking)

_(none — revisit G-1…G-3 only if product overrides the defaults above)_
