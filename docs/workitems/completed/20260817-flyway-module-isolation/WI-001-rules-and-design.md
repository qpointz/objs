# WI-001 — RULES.md Flyway section + design lock

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Process + design lock  
**Status:** complete  
**Depends on:** WI-000

## Goal

Make the two-Flyway contract **normative for every later story** by writing it into process rules, and point living persistence design at the same lock.

## Deliverables

- [x] New section **Flyway (library + derived apps)** in [`docs/workitems/RULES.md`](../../RULES.md) after Module Reference and before Concrete example integration
- [x] Pointer row under Concrete example integration → What “integrated” means (objs `bom_*` vs app tables)
- [x] Update [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md) Flyway paragraph to two lines + `{vendor}` SQL + greenfield
- [x] Confirm [`GAPS.md`](GAPS.md) matches RULES (no new open questions)

## RULES.md content (normative)

Must include:

- Two independent Flyway lines (objs vs derived app); both may use `V1`
- `{vendor}` = Spring Boot `DatabaseDriver` id (`postgresql`, `h2`, …)
- Order: objs first, then Boot Flyway, then JPA validate — version numbers do not control order
- Derived apps never add objs locations to `spring.flyway.locations`
- Foundation stories that change `bom_*` add objs SQL (`postgresql` **and** `h2`) in objs-core
- App-owned tables: next version on **that app’s** line only
- Examples are derived apps; workbench with no app DDL: `spring.flyway.enabled=false`
- Greenfield until existing DBs are gone
- Named anti-patterns: one merged history; reserved ranges (`V100`); objs folders on Boot locations

Do **not** change `AGENTS.md` (it already points at RULES.md).

## Out of scope

- Product code / SQL files (WI-002)
- Example migration rewrite (WI-003)

## Acceptance

- A later agent reading only RULES.md would not put objs scripts on `spring.flyway.locations` or continue objs version numbers in SBOM/AR
- G-A11 resolved in practice (section exists)
