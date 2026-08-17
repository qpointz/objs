# Graph and retrieval — multi-BOM versions (D-8)

**Status:** stub — fill in **WI-002**. Product glossary: WI-001 / [`docs/design/sbom/example.md`](../../../design/sbom/example.md). Gaps: [`GAPS.md`](GAPS.md).

## Intent

Map product concepts (Application, Version, Draft target, SBOM constituent, Combined SBOM, Fingerprint) to objs **named graphs**, domain tables, and rebuild/copy flows. Non-technical UI must not expose graph vocabulary.

## Sketch (to refine in WI-002)

```text
sbom_application
  └── sbom_application_version  (DRAFT|RELEASED; version=target or released id; based_on_version_id?)
        ├── graph_id → Combined SBOM (materialized aggregate, read-only)
        ├── sbom_application_sbom[]  (name, description, tags, graph_id, sort_order)
        │     └── each graph_id → constituent named graph (editable if DRAFT)
        └── sbom_application_fingerprint[] → snapshot copy of aggregate
```

## Open for WI-002

- Exact rebuild algorithm (membership + edge collapse)  
- Draft-from-based-on deep copy of constituents (see G-Q5)  
- Annotation keys on graphs (`kind`, `applicationId`, constituent id?)  
- MI / depends-on / CDX entry points stay on version aggregate  
- Migration: existing `version.graph_id` → first constituent; new aggregate copy on version
