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
- CRUD policies (in-memory repo default)
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

## Engine

C-31 is **DROOLS only** (UI badges may show kind/outcome). CUSTOM play deferred.

## Example policies

Paste-ready DROOLS sketches for the SBOM ontology / demo graphs: [`examples-sbom.md`](examples-sbom.md).
