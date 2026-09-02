# Policy evaluation (foundation)

**Status:** draft (planned story **C-24** [`objs-policy`](../../workitems/planned/objs-policy/STORY.md))  
**Normative locks:** story **WI-001**; until then treat design text as provisional.  
**Audience:** objs foundation embedders; example-app authors  
**Not this folder:** product compliance UX, regulatory catalogs, SBOM Application workflows

## Documents

| Doc | Contents |
|-----|----------|
| [**overview.md**](overview.md) | **Detailed draft** — philosophy, model, suites, applicability, findings, seeds, engines, roll-up, **thin batch/result pack**; mermaid diagrams |
| This README | Short index and pointers |

## One-line summary

`GraphFragment` → resolve → enrich → **select applicable** → evaluate (Drools first) → unified result with **findings** (0..n nodes/edges) and optional **suite folder roll-up**. Optional **batch pack** fans out opaque subjects for product matrices. Policies and suites are stored artefacts with a **seed format**; product rule content and assessment grids stay in apps.

## Related

- Story: [`planned/objs-policy/STORY.md`](../../workitems/planned/objs-policy/STORY.md)  
- Gaps: [`GAPS.md`](../../workitems/planned/objs-policy/GAPS.md)  
- Scenarios: [`EXAMPLES.md`](../../workitems/planned/objs-policy/EXAMPLES.md)  
- Fragments: [`../graph/fragments-and-analysis.md`](../graph/fragments-and-analysis.md)  
- Seeds: [`../graph/seeds.md`](../graph/seeds.md)  
- Apps vs foundation: [`../graph/apps-vs-foundation.md`](../graph/apps-vs-foundation.md)
