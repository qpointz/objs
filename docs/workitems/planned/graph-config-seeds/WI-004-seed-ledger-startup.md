# WI-004 — Seed ledger and startup loading

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Seed implementation  
**Status:** pending  
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

## Acceptance

- [ ] First startup imports configured resources in declaration order
- [ ] Restart with identical bytes skips completed resources
- [ ] Changed bytes trigger a new import and update the successful fingerprint
- [ ] Partial or failed imports are never recorded as completed
- [ ] `fail-fast` aborts startup and `continue` proceeds to later resources
- [ ] Ledger keys do not retain URL credentials or volatile query strings
- [ ] Startup ordering tests prove catalogs are ready before graph seed validation

