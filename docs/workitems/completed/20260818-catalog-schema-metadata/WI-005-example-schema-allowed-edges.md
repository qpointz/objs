# WI-005 — Example schema browse: read-only allowed edges

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Example schema UI  
**Status:** complete  
**Depends on:** WI-002 (catalog fields), WI-004 (seed verbs/descriptions worth showing)

## Goal

On each object schema in the **example** schema browsers, show a read-only **Allowed edges** section so a user can see inbound and outbound allow-list rules for that type. Content follows the workbench schema-edges listing; chrome follows the example UIs, not the workbench editor.

## Context

[`examples/sbom/sbom-service-ui`](../../../../examples/sbom/sbom-service-ui) and [`examples/asset-repository/asset-repository-service-ui`](../../../../examples/asset-repository/asset-repository-service-ui) already share nearly the same read-only schema page ([`SchemaViewPage.tsx`](../../../../examples/sbom/sbom-service-ui/src/SchemaViewPage.tsx) / AR copy): header, version select, Visual / JSON / YAML, [`SchemaTreeView`](../../../../examples/sbom/sbom-service-ui/src/SchemaTreeView.tsx) of `contentSchema` only. They do **not** show allow-list rules.

Workbench reference (what to **list**, not how to style): [`ObjectEdgesEditor.tsx`](../../../../objs-service-ui/src/ObjectEdgesEditor.tsx) on the schema explorer **Edges** tab — direction, source type, role, target type, cardinality, properties policy + properties schema. This story also adds description, `sourceVerb`, `targetVerb`, tags, attributes — show those when present.

SBOM already has a product-shaped `GET .../asset-types/{type}/relationships` used by related-assets, not schema browse; it omits wildcards and most metadata. AR has no equivalent on the schema catalog API.

## Deliverables

- [x] Domain read API (or extend existing) on **both** example services: inbound + outbound allowed-edge rules for a type, including `*` wildcards (same match as workbench `edgesForType`), with cardinality, properties policy/schema, description, verbs, tags, attributes
- [x] Read-only **Allowed edges** block on **both** `SchemaViewPage`s (Visual page chrome — not inside the JSON/YAML schema document)
- [x] Example Mantine look (titles, dimmed empty state, table or stacked papers like schema cards). **No** workbench edit form, action icons, or relationship-graph chrome
- [x] Type names link to `/schemas/{type}` when that type exists in the catalog
- [x] Empty state when the type has no matching rules
- [x] Keep SBOM and AR schema pages in parity (copy pattern; no new shared UI package)

## Out of scope

- Editing allow-list rules from example apps (workbench / WI-003)
- Extracting a shared frontend library
- Changing related-assets instance UI (`RelatedAssets.tsx`)

## Acceptance

- Opening an object schema in SBOM and AR schema browse shows inbound/outbound allow-list rows with the workbench information set (plus new metadata when seeded)
- JSON/YAML views remain the schema document; allowed edges stay a separate read-only section
- Styling matches the example schema pages, not workbench
- `./gradlew :sbom-service:test :asset-repository-service:test` (plus UI tests if added)
