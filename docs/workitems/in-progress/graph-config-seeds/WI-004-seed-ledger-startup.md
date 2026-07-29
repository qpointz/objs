# WI-004 — Seed ledger and startup loading

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Seed implementation  
**Status:** done  
**Depends on:** WI-003; G-S6–G-S9 in [`GAPS.md`](GAPS.md)

## Goal

Load ordered seed resources at application startup and use a durable content-fingerprint ledger to
skip already-applied resources safely.

## Scope

- Add Flyway-managed `bom_seed_ledger` persistence and repository adapter
- Add configuration properties for ordered resources and failure behavior
- Resolve `classpath:` and `file:` resources through Spring resource loading
- Derive safe, stable ledger keys and raw-content fingerprints
- Run seed loading after persistence/catalog initialization and before normal application use
- Skip a resource only when its latest successful fingerprint matches
- Store completion only after a zero-error import; store failure diagnostics otherwise
- Implement `fail-fast` and `continue` behavior

## Configuration

```yaml
objs:
  seeds:
    enabled: true
    on-failure: FAIL_FAST   # or CONTINUE
    resources:
      - name: sbom-ontology
        location: classpath:seeds/sbom-ontology.yaml
      - name: sbom-demo
        location: classpath:seeds/sbom-demo-graph.yaml
```

## Acceptance

- [x] First startup imports configured resources in declaration order
- [x] Restart with identical bytes skips completed resources
- [x] Changed bytes trigger a new import and update the successful fingerprint
- [x] Partial or failed imports are never recorded as completed
- [x] `fail-fast` aborts startup and `continue` proceeds to later resources
- [x] Ledger keys do not retain URL credentials or volatile query strings
- [x] Startup ordering tests prove catalogs are ready before graph seed validation
