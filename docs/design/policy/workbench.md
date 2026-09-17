# Policy workbench playground (C-31)

**Status:** design lock for [policy-workbench](../../workitems/completed/20260904-policy-workbench/STORY.md).  
**Gaps:** [`GAPS.md`](../../workitems/completed/20260904-policy-workbench/GAPS.md) (G-P23…G-P23f).

## Intent

Basic **replaceable** Policy playground in `:objs-service-ui` — exercise DROOLS evaluate + compile check against the shared graph context. Not a compliance product. Keep HTTP seams stable when the page is rewritten later.

## Transport (`:objs-policy-service`)

Mirror jgrapht/gremlin:

- New module, Boot autoconfig, **not** on `:objs-service` by default
- Wire on `:objs-service-app` only
- OpenAPI tag `policy`
- `GET …/policy/capabilities` — UI soft-fail when absent (Policy nav stays visible)
- CRUD policies (JPA when persistence wired; in-memory fallback)
- `GET …/policy/export?format=seeds` — full catalog REPLACE seed YAML (C-28)
- `POST …/policy/check` — compile/validate body (DROOLS) → **Output** pane
- `POST …/policy/evaluate` — policy × fragment from graph context → **Output** pane

## UI chrome

```text
[Policies|Suites|Archives] | (mode-specific body)
```

- Nav **Policy** after Query, before Composer (`/policy`)
- Left pane: **Policies | Suites | Archives** mode tabs (not top Evaluate|Suites subnav)
- Graph pane: **Visual** (canvas; disabled over node cap) | **Data** (vertices/edges grid)
- Visual: hover **Filter** toolbar (Types / Edges / Severity / Reset) + Apply layout / Fit to view; filters dim the canvas
- Data: Vertices — Severity and Type **column funnel** menus (shared type/severity sets with Visual). Edges — Severity; **Type** funnel = edge schema types only; **Source Type** / **Target Type** / **Role** funnels (Role shared with Visual Edges filter); Source/Target name columns.
- Data rows show evaluation **Severity** and respect the same severity/type filters as Visual
- Add = blank DROOLS policy then edit; trash deletes; explicit **Save**; Check/Evaluate use editor buffer
- Title row: eval/exec stats sit left of actions (spacer from title); no overall PASS/FAIL pill on Policy
- Under Visual/Data (splitter): **Policy | Evaluations | Object** (Suites: **Selection | Evaluation | Object**). Selecting on Visual/Data opens **Object**. **Evaluations** is selection-sensitive (findings for the selected node/edge). Finding click pans/selects and stays on Evaluations.
- No Categories/Tags toolbar filters (tree search remains). No separate Object/Tasks side column.

Also: [`metadata.md`](metadata.md) (C-32 list navigation — **shipped**; General \| Code tabs. Toolbar category/tag filters removed in U-11).

## C-32 play extensions

- `GET/POST/PUT/DELETE …/policy/categories`
- `GET …/policy/policies?categoryId=&tag=&key=&annotation=k=v`
- Policy create/update require `categoryId`, `tags`, `key`, optional `name` / `annotations` / `version` (major.minor)

## C-27 Suites mode

Policy route (`/policy`) uses left **Policies | Suites | Archives** mode tabs:

| Item | Role |
|------|------|
| **Policies** | Policy play (C-31/C-32) |
| **Suites** | Suite/folder/matcher authoring, Examine selection, `evaluateSuite`; **Persist result** → archive |
| **Archives** | Read-only browse/inspect of persisted evaluation packs (C-39 / U-12) |

- Suites share the same layout: tree + full-height editor + Visual/Data + **Output**.
- HTTP (on `:objs-policy-service`): `…/policy/suites` CRUD, `POST …/suites/selection`, `POST …/suites/evaluate`.
- Design: [`suites.md`](suites.md).

## C-39 Archives mode (read-only)

Third **in-page** Policy mode — **not** a new L0 nav item. Soft-fails when capabilities lack `"archive"`.

```text
[Policies|Suites|Archives] | List summaries | Inspector (Results / Policies / Input)
```

| Persist axis | Inspector tab | Notes |
|--------------|---------------|-------|
| `results` | **Results** | Suite tree *or* flat outcomes; read-only reuse of Suites widgets |
| `executionContext` | **Policies** | Structured thin T₀ snapshot + raw JSON extras |
| `input` | **Input** | Visual \| Data \| Raw of frozen fragment; **does not** mutate shared graph context |

Tabs appear only when the axis flag is true **and** the payload is present. After Suites **Persist**, **Open archive** switches to this mode and loads the saved id.

### Archive HTTP (read + write)

| Method | Path | Role |
|--------|------|------|
| `POST` | `…/evaluations/suite` | Persist suite run (C-33) |
| `GET` | `…/evaluations` | List summaries (`limit`/`offset`/`kind`/`tag`) |
| `GET` | `…/evaluations/{id}` | Full document; `?view=standard` omits `input` |
| `DELETE` | `…/evaluations/{id}` | Delete pack |

Port: `EvaluationArchive.list` / `load` / `loadAsStandard` / `delete`. Story: [`policy-archive-workbench`](../../workitems/completed/20260917-policy-archive-workbench/STORY.md).

## C-28 Persistence + seeds + export

Normative: [`policy-seeds-persistence`](../../workitems/completed/20260906-policy-seeds-persistence/STORY.md).

- Catalog JPA behind the same repository ports when `UnitOfWork` is present (`key` identity).
- Seed kinds via `SeedDocumentHandler` (`Category` / `Policy` / `PolicySuite` + `Drop*`; apply/replace).
- **`GET …/policy/export?format=seeds`** — REPLACE YAML for full setup (categories, latest policies with inline body, suites).
- Workbench **Evaluate** toolbar: **Export** downloads that pack.

Evaluation **result** persistence is **[C-33 `policy-results-persistence`](../../workitems/completed/20260907-policy-results-persistence/STORY.md)** (out of C-28; see C-27 `RESULTS-MODEL`).

## Engine

C-31 is **DROOLS only** (UI badges may show kind/outcome). CUSTOM play deferred.

## Example policies

Paste-ready DROOLS sketches for the SBOM ontology / demo graphs: [`examples-sbom.md`](examples-sbom.md).
