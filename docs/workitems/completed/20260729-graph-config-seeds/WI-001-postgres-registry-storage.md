# WI-001 — PostgreSQL baseline and registry storage

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — PostgreSQL persistence  
**Status:** pending  
**Depends on:** G-P1, G-P3, G-P4 in [`GAPS.md`](GAPS.md)

## Goal

Verify the existing entity/edge persistence against PostgreSQL and introduce consumer-facing
registry/catalog abstractions, reusable in-memory implementations, and Flyway/JPA repository
storage for schemas and allowed-edge rules (G-P4).

## Scope

- Run the existing entity/edge persistence path against PostgreSQL and correct dialect, JSONB,
  Flyway, mapping, foreign-key, or transaction defects found
- Extract schema-catalog and allowed-edge-catalog **interfaces/base types** from today's concrete
  types; expose only these abstractions to consumers
- Put reusable edge matching/precedence behavior in shared base/helper code
- Provide an **in-memory** implementation (behavior-compatible with current catalogs)
- Add Flyway tables for:
  - schemas keyed by `(type, version)`, with JSONB schema document
  - allowed-edge rules keyed by `(source_type, role, target_type)`, including properties policy,
    empty-properties behavior, and referenced properties schema
- Provide JPA records, Spring Data repositories, and storage adapters used by the persistent
  write-through implementation in WI-002
- Preserve wildcard rule values and deterministic rule precedence across both implementations
- Add Testcontainers PostgreSQL integration tests; retain focused H2 / in-memory tests where useful

## Out of scope

- Production write-through cache composition and Spring bean wiring (WI-002)
- Seed format, importer, or ledger
- Registry REST contract changes

## Acceptance

- [ ] Existing entity and edge records round-trip on PostgreSQL, including JSONB fields
- [ ] Schema and allowed-edge catalog APIs are expressed as consumer-facing abstractions
- [ ] In-memory implementations preserve current behavior and shared edge matching semantics
- [ ] Flyway creates schema and allowed-edge-rule tables with required uniqueness and constraints
- [x] Schema repository CRUD round-trips complete authoritative DSL definitions
- [ ] Allowed-edge-rule repository CRUD round-trips every policy field and wildcard value
- [ ] Migrations and repositories pass Testcontainers PostgreSQL integration tests and retained H2 tests
- [ ] Design notes record PostgreSQL-specific choices and limitations discovered

