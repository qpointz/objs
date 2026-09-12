# WI-001 — Export rewrite for `.drl`

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Export DRL  
**Status:** done  
**Depends on:** WI-000  
**Examples:** **SBOM policy seeds** (`examples/sbom/.../seeds/policy/drl/*.drl`)

## Goal

Include Drools `.drl` in export text rewrite so `package` / `import` lines follow `TARGET_PACKAGE`.
Document that the repo scan found no other missing package-sensitive non-code extensions.

## Deliverables

- [x] Add `.drl` to `REPLACE_EXTENSIONS` in [`scripts/export/generate-config.py`](../../../../scripts/export/generate-config.py)
- [x] Fixture DRL + assertion in [`scripts/export/test_fixture.py`](../../../../scripts/export/test_fixture.py)
- [x] Note in [`scripts/export/README.md`](../../../../scripts/export/README.md)
- [x] `make test-export-fixture` passes

## Acceptance

- [x] Exported sample `.drl` has no leftover `org.poc.objs`
- [x] G-1 decision already locked; ship the `.drl` rewrite

## Out of scope

- Codegen path guard (WI-002)
