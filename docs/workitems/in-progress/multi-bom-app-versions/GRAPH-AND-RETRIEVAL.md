# Graph and retrieval — multi-BOM versions (D-8)

**Status:** stub — fill in **WI-002**. Product glossary: WI-001 / [`docs/design/sbom/example.md`](../../../design/sbom/example.md). Gaps: [`GAPS.md`](GAPS.md).

## Intent

Map product concepts (Application, Version, Draft target, SBOM constituent, Combined SBOM, Fingerprint) to objs **named graphs**, domain tables, and rebuild/copy flows. Non-technical UI must not expose graph vocabulary. Tags live on domain rows only (not graph payload).

## Sketch (to refine in WI-002)

```text
sbom_application                    tags[]
  └── sbom_application_version      tags[]; DRAFT|RELEASED; version=target or released;
                                    based_on_version_id?
        ├── graph_id → Combined SBOM (materialized aggregate, read-only)
        ├── sbom_application_sbom[] (name, description, tags[], graph_id, sort_order)
        │     └── each graph_id → constituent named graph (editable if DRAFT)
        └── sbom_application_fingerprint[]  name, category (approval|history|unknown)
              └── graph_id → copy of aggregate only (never constituent rows)
```

## Copy / snapshot (locked)

| Action | Graphs copied |
|--------|----------------|
| New draft, keep split | Each constituent graph + metadata; rebuild aggregate |
| New draft, combine | One constituent = copy of source **aggregate**; rebuild (identity) |
| Fingerprint | **Aggregate only** — no `sbom_application_sbom` on the fingerprint |

## Open for WI-002

- Exact rebuild algorithm (membership + edge collapse)  
- Annotation keys on graphs (`kind`, `applicationId`, constituent id?)  
- MI / depends-on / CDX entry points stay on version aggregate; latest = semver-max RELEASED (G-Q11)  
- Migration: existing `version.graph_id` → first constituent; new aggregate copy on version  
- Fingerprint annotations (`kind=application-fingerprint`)
