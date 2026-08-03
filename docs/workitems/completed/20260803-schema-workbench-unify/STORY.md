# Story: Schema workbench unify

**Slug:** `schema-workbench-unify`  
**Status:** completed  
**Branch:** `schema-workbench-unify`  
**Backlog:** U-1  
**Design:** [`docs/design/ui.md`](../../../design/ui.md)  
**Depends on:** cardinality UI on `allowed-edge-cardinality` (branch base)

## Goal

Merge Schema Explorer and Schema Linter into one Schemas workbench: browse and edit in place,
preserve lint / Visual / Expert / save / create-version, author allowed edges on **objects**
(not on edge-property schemas), and reclaim space with a horizontal top app nav (no left navbar).

## Confirmed decisions

| Topic | Choice |
|-------|--------|
| App chrome | Top horizontal nav: Graph explorer, Schemas, Object linter; remove app left navbar |
| Type list | Flat list; Object/Edge pill; denser/narrower; click → latest version |
| Create | Menu: Create New Object / Create New Edge (not “New draft”) |
| Version | Content toolbar: version select + Create version (split control) |
| Allowed edges | Edit on entity/object: add/edit/delete inbound and outbound (draft until Save) |
| New edge defaults | propertiesPolicy **NONE** by default; optional EDGE_PROPERTIES schema pick |
| Edge schemas | Edit payload DSL only — no relation authoring on the edge model |
| Linter route | Redirect `/linter` and `/schemas/.../lint` into Schemas flows |

## Stages

| Stage | Work items | Exit condition |
|-------|------------|----------------|
| 0 — App chrome + shell | WI-000 | Top nav, no navbar, redirects, schema shell |
| 1 — Type list | WI-001 | Flat O/E list + Create Object/Edge |
| 2 — Toolbar + editors | WI-002 | Version/create-version + in-place linter editors |
| 3 — Object edges | WI-003 | Object-level edge CRUD; edge-schema relations UI removed |
| 4 — Docs | WI-004 | ui.md + smoke tests |

## Work Items

- [x] WI-000 — App chrome + shell merge (`WI-000-app-chrome-shell.md`)
- [x] WI-001 — Type list + create menu (`WI-001-type-list-create.md`)
- [x] WI-002 — Toolbar version + in-place editing (`WI-002-toolbar-editors.md`)
- [x] WI-003 — Object-level allowed-edge editor (`WI-003-object-edge-editor.md`)
- [x] WI-004 — Docs + polish (`WI-004-docs-polish.md`)

## Scope

- [`AppLayout.tsx`](../../../objs-sbom-example/ui/src/AppLayout.tsx) header nav; drop navbar
- Unify explorer + linter into Schemas workbench
- Object-level allowed-edge create/delete via registry edges API
- Nav/docs cleanup

## Out of scope

- Backend allow-list / cardinality model changes
- Persist-time edge count enforcement
- Graph explorer canvas redesign
- Closing the cardinality story
