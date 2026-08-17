# Graph and retrieval — multi-BOM versions (D-8)

**Status:** stub — fill in **WI-002**. Product glossary: WI-001 / [`docs/design/sbom/example.md`](../../../design/sbom/example.md). Gaps: [`GAPS.md`](GAPS.md).

## Intent

Map product concepts (Application, Version, Draft target, **BOM**, **Combined SBOM**, Fingerprint) to objs **named graphs**, domain tables, and copy flows. Non-technical UI must not expose graph vocabulary. Tags live on domain rows only (not graph payload).

## Sketch (to refine in WI-002)

```text
sbom_application                    tags[]
  └── sbom_application_version      tags[]; DRAFT|RELEASED; version=target or released;
                                    based_on_version_id?
        ├── Combined SBOM → ephemeral union of all BOM graphs (not stored)
        ├── sbom_application_sbom[] (product: BOM; name, description, tags[], graph_id, sort_order)
        │     └── each graph_id → constituent named graph (editable if DRAFT)
        └── sbom_application_fingerprint[]  name, category (approval|history|unknown)
              └── graph_id → snapshot of Combined SBOM union (never BOM rows)
```

## Copy / snapshot (locked)

| Action | Graphs copied |
|--------|----------------|
| New draft, keep split | Each constituent graph + metadata; rebuild aggregate |
| New draft, combine | One constituent = copy of source **version aggregate** |
| New draft from fingerprint | One constituent = copy of **fingerprint** graph |
| Fingerprint | **Aggregate only** — no `sbom_application_sbom` on the fingerprint |

## Open for WI-002

- Exact rebuild algorithm (membership + edge collapse)  
- Annotation keys on graphs (`kind`, `applicationId`, constituent id?)  
- MI / depends-on / CDX entry points stay on version aggregate; latest = semver-max RELEASED (G-Q11)  
- Migration: existing `version.graph_id` → first constituent; new aggregate copy on version  
- Fingerprint annotations (`kind=application-fingerprint`)
