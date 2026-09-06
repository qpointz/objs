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
- `POST …/policy/check` — compile/validate body (DROOLS) → tasks **Policy** tab
- `POST …/policy/evaluate` — policy × fragment from graph context → **Evaluations** tab

## UI chrome

```text
Policies | Editor | Visual | Data | Object | Tasks(N)
         |        |← shared tabs →| +------ tabs -----+
         +======== tasks: Policy | Evaluations ======+
```

- Nav **Policy** after Query, before Composer (`/policy`)
- Graph pane: **Visual** (canvas; disabled over node cap) | **Data** (vertices/edges grid) share one content area
- Data rows show evaluation **Severity** and respect the same severity filter badges as Visual
- Add = blank DROOLS policy then edit; trash deletes; explicit **Save**; Check/Evaluate use editor buffer
- Right pane: **Object** (Explorer inspect) | **Tasks (N)** (findings for selection)
- Bottom click → pan/select node/edge + focus Tasks detail; no bottom selection-filter

Also: [`metadata.md`](metadata.md) (C-32 list navigation — **shipped** on play UI: category/tag filters; General \| Code tabs).

## C-32 play extensions

- `GET/POST/PUT/DELETE …/policy/categories`
- `GET …/policy/policies?categoryId=&tag=&key=&annotation=k=v`
- Policy create/update require `categoryId`, `tags`, `key`, optional `name` / `annotations` / `version` (major.minor)

## C-27 Suites subnav

Policy route (`/policy`) uses shared chrome with **subnav**:

| Item | Role |
|------|------|
| **Evaluate** | Existing Policy play (C-31/C-32) — unchanged |
| **Suites** | Suite/folder/matcher authoring, selection examine, `evaluateSuite` |

- Suites layout is **edit-first**: tree + center editor primary; graph canvas secondary (shared **GraphContextBar** only — no large live graph by default).
- HTTP (on `:objs-policy-service`): `…/policy/suites` CRUD, `POST …/suites/selection`, `POST …/suites/evaluate`.
- Design: [`suites.md`](suites.md).

## C-28 Persistence + seeds + export

Normative: [`policy-seeds-persistence`](../../workitems/in-progress/policy-seeds-persistence/STORY.md).

- Catalog JPA behind the same repository ports when `UnitOfWork` is present (`key` identity).
- Seed kinds via `SeedDocumentHandler` (`Category` / `Policy` / `PolicySuite` + `Drop*`; apply/replace).
- **`GET …/policy/export?format=seeds`** — REPLACE YAML for full setup (categories, latest policies with inline body, suites).
- Workbench **Evaluate** toolbar: **Export** downloads that pack.

Evaluation **result** persistence remains out of C-28 (see C-27 `RESULTS-MODEL`).

## Engine

C-31 is **DROOLS only** (UI badges may show kind/outcome). CUSTOM play deferred.

## Example policies

Paste-ready DROOLS sketches for the SBOM ontology / demo graphs: [`examples-sbom.md`](examples-sbom.md).
